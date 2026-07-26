using System.Runtime.InteropServices;

namespace Axis.Desktop.Controller;

public static class WindowsMouse
{
    [DllImport("user32.dll")]
    private static extern void mouse_event(uint dwFlags, uint dx, uint dy, uint dwData, UIntPtr dwExtraInfo);

    [DllImport("user32.dll")]
    private static extern bool GetCursorPos(out POINT lpPoint);

    [DllImport("user32.dll")]
    private static extern bool SetCursorPos(int X, int Y);

    [StructLayout(LayoutKind.Sequential)]
    private struct POINT { public int X; public int Y; }

    private const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
    private const uint MOUSEEVENTF_LEFTUP = 0x0004;
    private const uint MOUSEEVENTF_RIGHTDOWN = 0x0008;
    private const uint MOUSEEVENTF_RIGHTUP = 0x0006;
    private const uint MOUSEEVENTF_WHEEL = 0x0800;

    public static void MoveDelta(float deltaX, float deltaY)
    {
        if (!OperatingSystem.IsWindows()) return;
        if (MathF.Abs(deltaX) < 0.05f && MathF.Abs(deltaY) < 0.05f) return;
        if (GetCursorPos(out var p))
        {
            SetCursorPos(p.X + (int)MathF.Round(deltaX * 18f), p.Y - (int)MathF.Round(deltaY * 18f));
        }
    }

    public static void ClickLeft(bool down)
    {
        if (!OperatingSystem.IsWindows()) return;
        mouse_event(down ? MOUSEEVENTF_LEFTDOWN : MOUSEEVENTF_LEFTUP, 0, 0, 0, UIntPtr.Zero);
    }

    public static void ClickRight(bool down)
    {
        if (!OperatingSystem.IsWindows()) return;
        mouse_event(down ? MOUSEEVENTF_RIGHTDOWN : MOUSEEVENTF_RIGHTUP, 0, 0, 0, UIntPtr.Zero);
    }
}
