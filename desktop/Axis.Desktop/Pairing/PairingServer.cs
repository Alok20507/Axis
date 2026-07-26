using System.Buffers.Binary;
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

        try
        {
            // 1. Read Big-Endian Magic (0x41584953)
            var headerBuf = new byte[4];
            if (await ReadExactAsync(stream, headerBuf) != 4) return;
            if (BinaryPrimitives.ReadInt32BigEndian(headerBuf) != 0x41584953) return;

            // 2. Read Client Nonce (16 bytes)
            var clientNonce = new byte[16];
            if (await ReadExactAsync(stream, clientNonce) != 16) return;

            var activePin = CurrentPinCode;
            var salt = RandomNumberGenerator.GetBytes(16);
            var iv = RandomNumberGenerator.GetBytes(12);
            var key = Rfc2898DeriveBytes.Pbkdf2(activePin, salt, 150_000, HashAlgorithmName.SHA256, 32);

            // 3. Write Big-Endian Magic (0x41584953) + Salt (16B) + IV (12B)
            var responseHeader = new byte[4 + 16 + 12];
            BinaryPrimitives.WriteInt32BigEndian(responseHeader.AsSpan(0, 4), 0x41584953);
            salt.CopyTo(responseHeader, 4);
            iv.CopyTo(responseHeader, 20);
            await stream.WriteAsync(responseHeader);
            await stream.FlushAsync();

            // 4. Read Big-Endian Proof Length (4 bytes)
            var proofLenBuf = new byte[4];
            if (await ReadExactAsync(stream, proofLenBuf) != 4) return;
            var proofLen = BinaryPrimitives.ReadInt32BigEndian(proofLenBuf);
            if (proofLen <= 0 || proofLen > 512) return;

            // 5. Read Encrypted Proof bytes
            var proof = new byte[proofLen];
            if (await ReadExactAsync(stream, proof) != proofLen) return;

            // 6. Decrypt and Verify Proof
            var expected = Encoding.UTF8.GetBytes("axis-pair-v1");
            var decryptedProof = AesGcmDecrypt(key, iv, clientNonce, proof);
            if (!CryptographicOperations.FixedTimeEquals(decryptedProof, expected)) return;

            // 7. Pairing Success! Send Encrypted Session Key
            var session = RandomNumberGenerator.GetBytes(32);
            var encryptedSession = AesGcmEncrypt(key, iv, clientNonce, session);

            var sessionHeader = new byte[4 + encryptedSession.Length];
            BinaryPrimitives.WriteInt32BigEndian(sessionHeader.AsSpan(0, 4), encryptedSession.Length);
            encryptedSession.CopyTo(sessionHeader, 4);

            await stream.WriteAsync(sessionHeader);
            await stream.FlushAsync();

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

    private static async Task<int> ReadExactAsync(NetworkStream stream, byte[] buffer)
    {
        var totalRead = 0;
        while (totalRead < buffer.Length)
        {
            var read = await stream.ReadAsync(buffer.AsMemory(totalRead, buffer.Length - totalRead));
            if (read == 0) break;
            totalRead += read;
        }
        return totalRead;
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
