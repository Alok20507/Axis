namespace Axis.Desktop.Controller;

public sealed class ControllerRuntime(IVirtualGamepadBackend backend)
{
    private readonly IVirtualGamepadBackend _backend = backend;
    private XInputReport _lastReport;

    public XInputReport LastReport => _lastReport;

    public async ValueTask ApplyAsync(NormalizedControllerState state, CancellationToken cancellationToken)
    {
        // 1. Submit XInput Xbox 360 Controller Report
        if (!_backend.IsConnected)
        {
            try { await _backend.ConnectAsync(cancellationToken).ConfigureAwait(false); } catch { }
        }

        var report = XInputMapper.Map(state);
        try { await _backend.SubmitAsync(report, cancellationToken).ConfigureAwait(false); } catch { }
        _lastReport = report;

        // 2. Windows Mouse Remote Cursor Movement (Right Stick / Trackpad)
        if (MathF.Abs(state.RightStickX) > 0.04f || MathF.Abs(state.RightStickY) > 0.04f)
        {
            WindowsMouse.MoveDelta(state.RightStickX, state.RightStickY, 36f);
        }

        // 3. Universal Windows Keyboard / Mouse Button Emulation (W/A/S/D + Clicks)
        if (OperatingSystem.IsWindows())
        {
            // WASD Movement
            WindowsKeyboard.SendKey(WindowsKeyboard.VK_W, state.LeftStickY > 0.35f);
            WindowsKeyboard.SendKey(WindowsKeyboard.VK_S, state.LeftStickY < -0.35f);
            WindowsKeyboard.SendKey(WindowsKeyboard.VK_A, state.LeftStickX < -0.35f);
            WindowsKeyboard.SendKey(WindowsKeyboard.VK_D, state.LeftStickX > 0.35f);

            // D-Pad Arrow Keys
            WindowsKeyboard.SendKey(WindowsKeyboard.VK_UP, (state.Buttons & XboxButtons.DPadUp) != 0);
            WindowsKeyboard.SendKey(WindowsKeyboard.VK_DOWN, (state.Buttons & XboxButtons.DPadDown) != 0);
            WindowsKeyboard.SendKey(WindowsKeyboard.VK_LEFT, (state.Buttons & XboxButtons.DPadLeft) != 0);
            WindowsKeyboard.SendKey(WindowsKeyboard.VK_RIGHT, (state.Buttons & XboxButtons.DPadRight) != 0);

            // Action Buttons
            bool btnA = (state.Buttons & XboxButtons.A) != 0;
            bool btnB = (state.Buttons & XboxButtons.B) != 0;
            bool btnX = (state.Buttons & XboxButtons.X) != 0;
            bool btnY = (state.Buttons & XboxButtons.Y) != 0;

            WindowsKeyboard.SendKey(WindowsKeyboard.VK_SPACE, btnA);
            WindowsKeyboard.SendKey(WindowsKeyboard.VK_SHIFT, btnB);
            WindowsKeyboard.SendKey(WindowsKeyboard.VK_R, btnX);
            WindowsKeyboard.SendKey(WindowsKeyboard.VK_E, btnY);

            // Mouse Clicks
            WindowsMouse.ClickLeft(btnA || state.Throttle > 0.5f);
            WindowsMouse.ClickRight(btnB || state.Brake > 0.5f);
        }
    }

    public ValueTask DisconnectAsync(CancellationToken cancellationToken) => _backend.DisconnectAsync(cancellationToken);
}
