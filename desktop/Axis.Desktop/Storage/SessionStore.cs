using System.Text.Json;

namespace Axis.Desktop.Storage;

public static class SessionStore
{
    private static readonly string FolderPath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Axis");
    private static readonly string FilePath = Path.Combine(FolderPath, "sessions.json");

    private static readonly List<byte[]> KnownKeys = [];

    static SessionStore()
    {
        Load();
    }

    public static IReadOnlyList<byte[]> GetSessionKeys() => KnownKeys;

    public static void SaveSessionKey(byte[] key)
    {
        if (key.Length != 32) return;
        if (KnownKeys.Any(k => k.SequenceEqual(key))) return;

        KnownKeys.Add(key);
        Save();
    }

    private static void Load()
    {
        try
        {
            if (!File.Exists(FilePath)) return;
            var json = File.ReadAllText(FilePath);
            var b64List = JsonSerializer.Deserialize<List<string>>(json);
            if (b64List == null) return;

            KnownKeys.Clear();
            foreach (var b64 in b64List)
            {
                var bytes = Convert.FromBase64String(b64);
                if (bytes.Length == 32) KnownKeys.Add(bytes);
            }
        }
        catch { }
    }

    private static void Save()
    {
        try
        {
            Directory.CreateDirectory(FolderPath);
            var b64List = KnownKeys.Select(Convert.ToBase64String).ToList();
            var json = JsonSerializer.Serialize(b64List);
            File.ReadAllText(FilePath);
            File.WriteAllText(FilePath, json);
        }
        catch { }
    }
}
