using System.Net;
using System.Net.Sockets;
using System.Security.Cryptography;
using System.Text;

namespace Axis.Desktop.Pairing;

public sealed class PairingServer(Action<string> onCode, Action<byte[]> onSession) : IAsyncDisposable
{
    public const int Port = 45101;
    private readonly CancellationTokenSource _stop = new();
    private TcpListener? _listener;
    public async Task RunAsync()
    {
        _listener = new TcpListener(IPAddress.Any, Port); _listener.Start();
        while (!_stop.IsCancellationRequested)
        {
            TcpClient client;
            try { client = await _listener.AcceptTcpClientAsync(_stop.Token); } catch (OperationCanceledException) { break; }
            _ = Task.Run(() => HandleAsync(client));
        }
    }
    private async Task HandleAsync(TcpClient client)
    {
        await using var stream = client.GetStream(); using var reader = new BinaryReader(stream, Encoding.UTF8, true); using var writer = new BinaryWriter(stream, Encoding.UTF8, true);
        try {
            if (reader.ReadInt32() != 0x41584953) return;
            var clientNonce = reader.ReadBytes(16); if (clientNonce.Length != 16) return;
            var code = RandomNumberGenerator.GetInt32(0, 1_000_000).ToString("D6"); onCode(code);
            var salt = RandomNumberGenerator.GetBytes(16); var iv = RandomNumberGenerator.GetBytes(12); var key = Rfc2898DeriveBytes.Pbkdf2(code, salt, 150_000, HashAlgorithmName.SHA256, 32);
            writer.Write(0x41584953); writer.Write(salt); writer.Write(iv); writer.Flush();
            var proof = reader.ReadBytes(reader.ReadInt32());
            var expected = Encoding.UTF8.GetBytes("axis-pair-v1");
            if (!CryptographicOperations.FixedTimeEquals(AesGcmDecrypt(key, iv, clientNonce, proof), expected)) return;
            var session = RandomNumberGenerator.GetBytes(32); var encrypted = AesGcmEncrypt(key, iv, clientNonce, session);
            writer.Write(encrypted.Length); writer.Write(encrypted); writer.Flush(); onSession(session);
        } catch (CryptographicException) { } finally { client.Close(); }
    }
    private static byte[] AesGcmEncrypt(byte[] key, byte[] iv, byte[] aad, byte[] plain) { var cipher = new byte[plain.Length]; var tag = new byte[16]; using var aes = new AesGcm(key, 16); aes.Encrypt(iv, plain, cipher, tag, aad); return cipher.Concat(tag).ToArray(); }
    private static byte[] AesGcmDecrypt(byte[] key, byte[] iv, byte[] aad, byte[] encrypted) { var plain = new byte[encrypted.Length - 16]; using var aes = new AesGcm(key, 16); aes.Decrypt(iv, encrypted[..^16], encrypted[^16..], plain, aad); return plain; }
    public ValueTask DisposeAsync() { _stop.Cancel(); _listener?.Stop(); _stop.Dispose(); return ValueTask.CompletedTask; }
}
