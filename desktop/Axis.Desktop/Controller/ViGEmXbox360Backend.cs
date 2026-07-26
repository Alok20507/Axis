using Nefarius.ViGEm.Client;
using Nefarius.ViGEm.Client.Targets;
using Nefarius.ViGEm.Client.Targets.Xbox360;

namespace Axis.Desktop.Controller;

/// Test backend for Windows 10/11. Requires the separately installed ViGEmBus driver.
public sealed class ViGEmXbox360Backend : IVirtualGamepadBackend
{
    private ViGEmClient? _client;
    private IXbox360Controller? _controller;
    public string DisplayName => "ViGEm virtual Xbox 360 controller";
    public bool IsConnected => _controller?.IsConnected == true;

    public ValueTask ConnectAsync(CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();
        if (IsConnected) return ValueTask.CompletedTask;
        _client = new ViGEmClient();
        _controller = _client.CreateXbox360Controller();
        _controller.Connect();
        return ValueTask.CompletedTask;
    }

    public ValueTask SubmitAsync(XInputReport report, CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();
        var controller = _controller ?? throw new InvalidOperationException("Virtual controller is not connected.");
        controller.SetAxisValue(Xbox360Axis.LeftThumbX, report.LeftThumbX);
        controller.SetSliderValue(Xbox360Slider.LeftTrigger, report.LeftTrigger);
        controller.SetSliderValue(Xbox360Slider.RightTrigger, report.RightTrigger);
        SetButtons(controller, report.Buttons);
        controller.SubmitReport();
        return ValueTask.CompletedTask;
    }

    public ValueTask DisconnectAsync(CancellationToken cancellationToken)
    {
        if (_controller?.IsConnected == true) _controller.Disconnect();
        _controller = null; _client?.Dispose(); _client = null;
        return ValueTask.CompletedTask;
    }
    public async ValueTask DisposeAsync() => await DisconnectAsync(CancellationToken.None);

    private static void SetButtons(IXbox360Controller controller, XboxButtons buttons)
    {
        foreach (var mapping in ButtonMap)
            controller.SetButtonState(mapping.button, (buttons & mapping.source) != 0);
    }
    private static readonly (Xbox360Button button, XboxButtons source)[] ButtonMap = [
        (Xbox360Button.A, XboxButtons.A), (Xbox360Button.B, XboxButtons.B), (Xbox360Button.X, XboxButtons.X), (Xbox360Button.Y, XboxButtons.Y),
        (Xbox360Button.Start, XboxButtons.Start), (Xbox360Button.Back, XboxButtons.Back), (Xbox360Button.LeftShoulder, XboxButtons.LeftShoulder), (Xbox360Button.RightShoulder, XboxButtons.RightShoulder),
        (Xbox360Button.Up, XboxButtons.DPadUp), (Xbox360Button.Down, XboxButtons.DPadDown), (Xbox360Button.Left, XboxButtons.DPadLeft), (Xbox360Button.Right, XboxButtons.DPadRight),
    ];
}
