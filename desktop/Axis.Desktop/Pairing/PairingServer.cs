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

    public string CurrentPinCode { get; private set; } = GeneratePin();

    private static string GeneratePin() => RandomNumberGenerator.GetInt32(0, 1_000_000).ToString("D6");

    public async Task RunAsync()
    {
        // Immediately notify UI of active PIN code on launch
        onCode(CurrentPinCode);

        _listener = new TcpListener(IPAddress.Any, Port);
        _listener.Start();

        while (!_stop.IsCancellationRequested)
        {
            TcpClient client;
            try { client = await _listener.AcceptTcpClientAsync(_stop.Token); } catch (OperationCanceledException) { break; }
            _ = Task.Run(() => HandleAsync(client));
        }
    }

    private async Task HandleAsync(TcpClient client)
    {
        await using var stream = client.GetStream();
        using var reader = new BinaryReader(stream, Encoding.UTF8, true);
        using var writer = new BinaryWriter(stream, Encoding.UTF8, true);

        try
        {
            if (reader.ReadInt32() != 0x41584953) return;
            var clientNonce = reader.ReadBytes(16);
            if (clientNonce.Length != 16) return;

            var activePin = CurrentPinCode;
            var salt = RandomNumberGenerator.GetBytes(16);
            var iv = RandomNumberGenerator.GetBytes(12);
            var key = Rfc2898DeriveBytes.Pbkdf2(activePin, salt, 150_000, HashAlgorithmName.SHA256, 32);

            writer.Write(0x41584953);
            writer.Write(salt);
            writer.Write(iv);
            writer.Flush();

            var proofLen = reader.ReadInt32();
            if (proofLen <= 0 || proofLen > 512) return;
            var proof = reader.ReadBytes(proofLen);

            var expected = Encoding.UTF8.GetBytes("axis-pair-v1");
            var decryptedProof = AesGcmDecrypt(key, iv, clientNonce, proof);
            if (!CryptographicOperations.FixedTimeEquals(decryptedProof, expected)) return;

            var session = RandomNumberGenerator.GetBytes(32);
            var encryptedSession = AesGcmEncrypt(key, iv, clientNonce, session);

            writer.Write(encryptedSession.Length);
            writer.Write(encryptedSession);
            writer.Flush();

            onSession(session);

            // Rotate PIN code for next phone connection
            CurrentPinCode = GeneratePin();
            onCode(CurrentPinCode);
        }
        catch (CryptographicException)
        {
            // Invalid PIN entered by phone
        }
        catch (Exception)
        {
            // Socket disconnected
        }
        finally
        {
            client.Close();
        }
    }

    private static byte[] AesGcmEncrypt(byte[] key, byte[] iv, byte[] aad, byte[] plain)
    {
        var cipher = new byte[plain.Length];
        var tag = new byte[16];
        using var aes = new AesGcm(key, 16);
        aes.Encrypt(iv, plain, cipher, tag, aad);
        return cipher.Concat(tag).ToArray();
    }

    private static byte[] AesGcmDecrypt(byte[] key, byte[] iv, byte[] aad, byte[] encrypted)
    {
        if (encrypted.Length < 16) return [];
        var plain = new byte[encrypted.Length - 16];
        using var aes = new AesGcm(key, 16);
        aes.Decrypt(iv, encrypted[..^16], encrypted[^16..], plain, aad);
        return plain;
    }

    public ValueTask DisposeAsync()
    {
        _stop.Cancel();
        _listener?.Stop();
        _stop.Dispose();
        return ValueTask.CompletedTask;
    }
}
