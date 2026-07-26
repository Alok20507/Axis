using System.Buffers.Binary;
using System.Net;
using System.Net.Sockets;
using System.Security.Cryptography;
using Axis.Desktop.Controller;
using Axis.Desktop.Storage;

namespace Axis.Desktop.Networking;

/// Receives fixed-width Android control frames (with optional AES-GCM encryption).
public sealed class ControlReceiver(ControllerRuntime runtime, Action<string>? onStatusChanged = null) : IAsyncDisposable
{
    public const int Port = 45102;
    private readonly CancellationTokenSource _stop = new();
    private UdpClient? _socket;
    public byte[]? SessionKey { get; set; }
    private bool _hasReportedConnection;

    public async Task RunAsync()
    {
        _socket = new UdpClient(new IPEndPoint(IPAddress.Any, Port));
        while (!_stop.IsCancellationRequested)
        {
            UdpReceiveResult datagram;
            try { datagram = await _socket.ReceiveAsync(_stop.Token).ConfigureAwait(false); }
            catch (OperationCanceledException) { break; }

            if (!TryDecode(datagram.Buffer, ref stateKey, out var state)) continue;

            if (!_hasReportedConnection)
            {
                _hasReportedConnection = true;
                onStatusChanged?.Invoke("Encrypted AES-256-GCM Session Active (120 Hz)");
            }

            // Move Windows Mouse when in Mouse Mode or right stick dragging
            if (MathF.Abs(state.RightStickX) > 0.08f || MathF.Abs(state.RightStickY) > 0.08f)
            {
                WindowsMouse.MoveDelta(state.RightStickX, state.RightStickY);
            }

            try { await runtime.ApplyAsync(state, _stop.Token).ConfigureAwait(false); }
            catch (InvalidOperationException) { /* backend unavailable */ }
        }
    }

    private byte[]? stateKey;

    private bool TryDecode(ReadOnlySpan<byte> buffer, ref byte[]? activeKey, out NormalizedControllerState state)
    {
        state = NormalizedControllerState.Neutral;
        ReadOnlySpan<byte> frame = buffer;

        // Try decrypting with current SessionKey or saved SessionStore keys
        if (buffer.Length >= 12 + 4 + 36)
        {
            var candidates = new List<byte[]>();
            if (SessionKey != null) candidates.Add(SessionKey);
            if (activeKey != null && !candidates.Contains(activeKey)) candidates.Add(activeKey);
            candidates.AddRange(SessionStore.GetSessionKeys().Where(k => !candidates.Contains(k)));

            foreach (var candidate in candidates)
            {
                try
                {
                    var iv = buffer[..12];
                    var cipherLength = BinaryPrimitives.ReadInt32BigEndian(buffer[12..16]);
                    if (cipherLength > 0 && buffer.Length >= 16 + cipherLength)
                    {
                        var cipherTextWithTag = buffer[16..(16 + cipherLength)];
                        var plain = new byte[cipherLength - 16];
                        using var aes = new AesGcm(candidate, 16);
                        aes.Decrypt(iv, cipherTextWithTag[..^16], cipherTextWithTag[^16..], plain);
                        frame = plain;
                        SessionKey = candidate;
                        activeKey = candidate;
                        SessionStore.SaveSessionKey(candidate);
                        break;
                    }
                }
                catch (CryptographicException)
                {
                    // Try next candidate key
                }
            }
        }

        if (frame.Length < 36 || BinaryPrimitives.ReadUInt32BigEndian(frame) != 0x524C4E4B || BinaryPrimitives.ReadUInt16BigEndian(frame[4..]) != 1) return false;

        float lsX = 0f, lsY = 0f, rsX = 0f, rsY = 0f, throttle = 0f, brake = 0f, handbrake = 0f;
        ushort buttonFlags = 0;

        if (frame.Length >= 48)
        {
            lsX = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[20..]));
            lsY = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[24..]));
            rsX = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[28..]));
            rsY = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[32..]));
            throttle = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[36..]));
            brake = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[40..]));
            handbrake = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[44..]));
            buttonFlags = BinaryPrimitives.ReadUInt16BigEndian(frame[48..]);
        }
        else if (frame.Length >= 46)
        {
            lsX = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[20..]));
            lsY = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[24..]));
            rsX = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[28..]));
            rsY = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[32..]));
            throttle = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[36..]));
            brake = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[40..]));
            buttonFlags = BinaryPrimitives.ReadUInt16BigEndian(frame[44..]);
        }
        else
        {
            lsX = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[20..]));
            throttle = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[24..]));
            brake = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[28..]));
            handbrake = BitConverter.Int32BitsToSingle(BinaryPrimitives.ReadInt32BigEndian(frame[32..]));
            buttonFlags = frame.Length >= 38 ? BinaryPrimitives.ReadUInt16BigEndian(frame[36..]) : (ushort)0;
        }

        if (!float.IsFinite(lsX) || !float.IsFinite(lsY) || !float.IsFinite(rsX) || !float.IsFinite(rsY) || !float.IsFinite(throttle) || !float.IsFinite(brake)) return false;

        state = new(
            Math.Clamp(lsX, -1f, 1f),
            Math.Clamp(lsY, -1f, 1f),
            Math.Clamp(rsX, -1f, 1f),
            Math.Clamp(rsY, -1f, 1f),
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
