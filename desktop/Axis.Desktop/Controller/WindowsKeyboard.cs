using System.Runtime.InteropServices;

namespace Axis.Desktop.Controller;

public static class WindowsKeyboard
{
    [DllImport("user32.dll")]
    private static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);

    private const uint KEYEVENTF_EXTENDEDKEY = 0x0001;
    private const uint KEYEVENTF_KEYUP = 0x0002;

    // Virtual-Key codes
    public const byte VK_W = 0x57;
    public const byte VK_A = 0x41;
    public const byte VK_S = 0x53;
    public const byte VK_D = 0x44;
    public const byte VK_SPACE = 0x20;
    public const byte VK_SHIFT = 0x10;
    public const byte VK_CONTROL = 0x11;
    public const byte VK_E = 0x45;
    public const byte VK_F = 0x46;
    public const byte VK_R = 0x52;
    public const byte VK_ESCAPE = 0x1B;
    public const byte VK_RETURN = 0x0D;
    public const byte VK_UP = 0x26;
    public const byte VK_DOWN = 0x28;
    public const byte VK_LEFT = 0x25;
    public const byte VK_RIGHT = 0x27;

    private static readonly HashSet<byte> ActiveKeys = [];

    public static void SendKey(byte vkCode, bool down)
    {
        if (!OperatingSystem.IsWindows()) return;
        lock (ActiveKeys)
        {
            if (down)
            {
                if (ActiveKeys.Add(vkCode))
                    keybd_event(vkCode, 0, KEYEVENTF_EXTENDEDKEY, UIntPtr.Zero);
            }
            else
            {
                if (ActiveKeys.Remove(vkCode))
                    keybd_event(vkCode, 0, KEYEVENTF_EXTENDEDKEY | KEYEVENTF_KEYUP, UIntPtr.Zero);
            }
        }
    }
}
