using System.Buffers.Binary;
using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using System.Text;

namespace Axis.Desktop.Networking;

public sealed class DiscoveryResponder(Action<string> onDiscovery) : IAsyncDisposable
{
    public const int Port = 45100;
    private readonly CancellationTokenSource _stop = new();
    private UdpClient? _socket;

    public async Task RunAsync()
    {
        _socket = new UdpClient(new IPEndPoint(IPAddress.Any, Port)) { EnableBroadcast = true };
        while (!_stop.IsCancellationRequested)
        {
            UdpReceiveResult received;
            try { received = await _socket.ReceiveAsync(_stop.Token).ConfigureAwait(false); }
            catch (OperationCanceledException) { break; }
            if (!TryReadDiscovery(received.Buffer, out var nonce)) continue;
            var response = CreatePacket(2, CreateResponsePayload(nonce));
            await _socket.SendAsync(response, response.Length, received.RemoteEndPoint).ConfigureAwait(false);
            onDiscovery($"Last discovery: {received.RemoteEndPoint.Address} at {DateTimeOffset.Now:t}");
        }
    }

    private static bool TryReadDiscovery(ReadOnlySpan<byte> data, out long nonce)
    {
        nonce = 0;
        if (!TryReadPacket(data, out var type, out var payload) || type != 1 || payload.Length != 8) return false;
        nonce = BinaryPrimitives.ReadInt64BigEndian(payload); return true;
    }

    private static byte[] CreateResponsePayload(long nonce)
    {
        var name = Encoding.UTF8.GetBytes(Environment.MachineName);
        if (name.Length is 0 or > 64) throw new InvalidOperationException("Computer name cannot be advertised.");
        var result = new byte[13 + name.Length];
        BinaryPrimitives.WriteInt64BigEndian(result, nonce);
        BinaryPrimitives.WriteUInt16BigEndian(result.AsSpan(8), 45101);
        BinaryPrimitives.WriteUInt16BigEndian(result.AsSpan(10), 1);
        result[12] = (byte)name.Length; name.CopyTo(result, 13); return result;
    }

    private static byte[] CreatePacket(byte type, byte[] payload)
    {
        var packet = new byte[24 + payload.Length]; var span = packet.AsSpan();
        BinaryPrimitives.WriteUInt32BigEndian(span, 0x52434C4B); span[4] = 1; span[5] = type;
        BinaryPrimitives.WriteUInt16BigEndian(span[6..], (ushort)payload.Length);
        BinaryPrimitives.WriteUInt32BigEndian(span[8..], 0);
        BinaryPrimitives.WriteInt64BigEndian(span[12..], Stopwatch.GetTimestamp());
        payload.CopyTo(packet, 20);
        BinaryPrimitives.WriteUInt32BigEndian(span[(20 + payload.Length)..], Crc32(span[..(20 + payload.Length)]));
        return packet;
    }

    private static bool TryReadPacket(ReadOnlySpan<byte> packet, out byte type, out ReadOnlySpan<byte> payload)
    {
        type = 0; payload = default;
        if (packet.Length < 24 || BinaryPrimitives.ReadUInt32BigEndian(packet) != 0x52434C4B || packet[4] != 1) return false;
        var length = BinaryPrimitives.ReadUInt16BigEndian(packet[6..]);
        if (packet.Length != 24 + length || Crc32(packet[..^4]) != BinaryPrimitives.ReadUInt32BigEndian(packet[^4..])) return false;
        type = packet[5]; payload = packet.Slice(20, length); return true;
    }

    private static uint Crc32(ReadOnlySpan<byte> bytes)
    {
        uint crc = 0xffffffff; foreach (var value in bytes) { crc ^= value; for (var bit = 0; bit < 8; bit++) crc = (crc >> 1) ^ ((crc & 1) == 1 ? 0xedb88320 : 0); } return ~crc;
    }
    public ValueTask DisposeAsync() { _stop.Cancel(); _socket?.Dispose(); _stop.Dispose(); return ValueTask.CompletedTask; }
}
