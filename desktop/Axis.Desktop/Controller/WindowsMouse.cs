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
    private const uint MOUSEEVENTF_RIGHTUP = 0x0010;

    private static float _accumX = 0f;
    private static float _accumY = 0f;
    private static bool _leftDown = false;
    private static bool _rightDown = false;

    public static void MoveDelta(float deltaX, float deltaY, float sensitivity = 32f)
    {
        if (!OperatingSystem.IsWindows()) return;
        if (MathF.Abs(deltaX) < 0.02f && MathF.Abs(deltaY) < 0.02f) return;

        // Non-linear response curve for ultra-precise micro-movement & fast flicks
        float signX = MathF.Sign(deltaX);
        float signY = MathF.Sign(deltaY);
        float magX = MathF.Pow(MathF.Abs(deltaX), 1.35f) * sensitivity;
        float magY = MathF.Pow(MathF.Abs(deltaY), 1.35f) * sensitivity;

        _accumX += signX * magX;
        _accumY += signY * magY;

        int stepX = (int)_accumX;
        int stepY = (int)_accumY;

        if (stepX != 0 || stepY != 0)
        {
            _accumX -= stepX;
            _accumY -= stepY;

            if (GetCursorPos(out var p))
            {
                SetCursorPos(p.X + stepX, p.Y - stepY);
            }
        }
    }

    public static void ClickLeft(bool down)
    {
        if (!OperatingSystem.IsWindows()) return;
        if (_leftDown == down) return;
        _leftDown = down;
        mouse_event(down ? MOUSEEVENTF_LEFTDOWN : MOUSEEVENTF_LEFTUP, 0, 0, 0, UIntPtr.Zero);
    }

    public static void ClickRight(bool down)
    {
        if (!OperatingSystem.IsWindows()) return;
        if (_rightDown == down) return;
        _rightDown = down;
        mouse_event(down ? MOUSEEVENTF_RIGHTDOWN : MOUSEEVENTF_RIGHTUP, 0, 0, 0, UIntPtr.Zero);
    }
}
