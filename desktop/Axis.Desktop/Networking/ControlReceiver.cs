using System.Buffers.Binary;
using System.Net;
using System.Net.Sockets;
using System.Security.Cryptography;
using Axis.Desktop.Controller;

namespace Axis.Desktop.Networking;

/// Receives fixed-width Android control frames (with optional AES-GCM encryption).
public sealed class ControlReceiver(ControllerRuntime runtime) : IAsyncDisposable
{
    public const int Port = 45102;
    private readonly CancellationTokenSource _stop = new();
    private UdpClient? _socket;
    public byte[]? SessionKey { get; set; }

    public async Task RunAsync()
    {
        _socket = new UdpClient(new IPEndPoint(IPAddress.Any, Port));
        while (!_stop.IsCancellationRequested)
        {
            UdpReceiveResult datagram;
            try { datagram = await _socket.ReceiveAsync(_stop.Token).ConfigureAwait(false); }
            catch (OperationCanceledException) { break; }

            if (!TryDecode(datagram.Buffer, SessionKey, out var state)) continue;
            try { await runtime.ApplyAsync(state, _stop.Token).ConfigureAwait(false); }
            catch (InvalidOperationException) { /* backend unavailable */ }
        }
    }

    private static bool TryDecode(ReadOnlySpan<byte> buffer, byte[]? sessionKey, out NormalizedControllerState state)
    {
        state = NormalizedControllerState.Neutral;
        ReadOnlySpan<byte> frame = buffer;

        // AES-GCM Encrypted packet handling (IV: 12B + Len: 4B + CipherText: N)
        if (sessionKey != null && buffer.Length >= 12 + 4 + 38)
        {
            try
            {
                var iv = buffer[..12];
                var cipherLength = BinaryPrimitives.ReadInt32BigEndian(buffer[12..16]);
                if (cipherLength > 0 && buffer.Length >= 16 + cipherLength)
                {
                    var cipherTextWithTag = buffer[16..(16 + cipherLength)];
                    var plain = new byte[cipherLength - 16];
                    using var aes = new AesGcm(sessionKey, 16);
                    aes.Decrypt(iv, cipherTextWithTag[..^16], cipherTextWithTag[^16..], plain);
                    frame = plain;
                }
            }
            catch (CryptographicException)
            {
                // Fall back to plain check if key mismatch
            }
        }

        if (frame.Length < 36 || BinaryPrimitives.ReadUInt32BigEndian(frame) != 0x524C4E4B || BinaryPrimitives.ReadUInt16BigEndian(frame[4..]) != 1) return false;

        var steering = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[20..]));
        var throttle = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[24..]));
        var brake = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[28..]));
        var handbrake = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[32..]));
        var buttonFlags = frame.Length >= 38 ? BinaryPrimitives.ReadUInt16BigEndian(frame[36..]) : (ushort)0;

        if (!float.IsFinite(steering) || !float.IsFinite(throttle) || !float.IsFinite(brake) || !float.IsFinite(handbrake)) return false;

        state = new(
            Math.Clamp(steering, -1f, 1f),
            Math.Clamp(throttle, 0f, 1f),
            Math.Clamp(brake, 0f, 1f),
            Math.Clamp(handbrake, 0f, 1f),
            (XboxButtons)buttonFlags
        );
        return true;
    }

    public async ValueTask DisposeAsync()
    {
        _stop.Cancel();
        _socket?.Dispose();
        await runtime.DisconnectAsync(CancellationToken.None);
        _stop.Dispose();
    }
}
