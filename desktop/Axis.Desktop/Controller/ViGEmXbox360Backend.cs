using Nefarius.ViGEm.Client;
using Nefarius.ViGEm.Client.Targets;
using Nefarius.ViGEm.Client.Targets.Xbox360;

namespace Axis.Desktop.Controller;

/// Virtual Xbox 360 controller backend for Windows 10/11 using Nefarius.ViGEm.Client.
public sealed class ViGEmXbox360Backend : IVirtualGamepadBackend
{
    private ViGEmClient? _client;
    private IXbox360Controller? _controller;
    private bool _isConnected;

    public string DisplayName => "ViGEm virtual Xbox 360 controller";
    public bool IsConnected => _isConnected;

    public ValueTask ConnectAsync(CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();
        if (_isConnected) return ValueTask.CompletedTask;

        try
        {
            _client = new ViGEmClient();
            _controller = _client.CreateXbox360Controller();
            _controller.Connect();
            _isConnected = true;
        }
        catch (Exception ex)
        {
            _isConnected = false;
            System.Diagnostics.Debug.WriteLine($"ViGEmBus connection error: {ex.Message}");
        }

        return ValueTask.CompletedTask;
    }

    public ValueTask SubmitAsync(XInputReport report, CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();
        if (!_isConnected || _controller == null) return ValueTask.CompletedTask;

        try
        {
            // Dual Thumbsticks
            _controller.SetAxisValue(Xbox360Axis.LeftThumbX, report.LeftThumbX);
            _controller.SetAxisValue(Xbox360Axis.LeftThumbY, report.LeftThumbY);
            _controller.SetAxisValue(Xbox360Axis.RightThumbX, report.RightThumbX);
            _controller.SetAxisValue(Xbox360Axis.RightThumbY, report.RightThumbY);

            // Analog Triggers
            _controller.SetSliderValue(Xbox360Slider.LeftTrigger, report.LeftTrigger);
            _controller.SetSliderValue(Xbox360Slider.RightTrigger, report.RightTrigger);

            // Digital Buttons & D-Pad
            SetButtons(_controller, report.Buttons);

            _controller.SubmitReport();
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"ViGEmBus submit error: {ex.Message}");
        }

        return ValueTask.CompletedTask;
    }

    public ValueTask DisconnectAsync(CancellationToken cancellationToken)
    {
        if (_isConnected)
        {
            try { _controller?.Disconnect(); } catch { }
            _isConnected = false;
        }
        _controller = null;
        try { _client?.Dispose(); } catch { }
        _client = null;
        return ValueTask.CompletedTask;
    }

    public async ValueTask DisposeAsync() => await DisconnectAsync(CancellationToken.None);

    private static void SetButtons(IXbox360Controller controller, XboxButtons buttons)
    {
        foreach (var mapping in ButtonMap)
            controller.SetButtonState(mapping.button, (buttons & mapping.source) != 0);
    }

    private static readonly (Xbox360Button button, XboxButtons source)[] ButtonMap = [
        (Xbox360Button.A, XboxButtons.A),
        (Xbox360Button.B, XboxButtons.B),
        (Xbox360Button.X, XboxButtons.X),
        (Xbox360Button.Y, XboxButtons.Y),
        (Xbox360Button.Start, XboxButtons.Start),
        (Xbox360Button.Back, XboxButtons.Back),
        (Xbox360Button.LeftThumb, XboxButtons.LeftThumb),
        (Xbox360Button.RightThumb, XboxButtons.RightThumb),
        (Xbox360Button.LeftShoulder, XboxButtons.LeftShoulder),
        (Xbox360Button.RightShoulder, XboxButtons.RightShoulder),
        (Xbox360Button.Up, XboxButtons.DPadUp),
        (Xbox360Button.Down, XboxButtons.DPadDown),
        (Xbox360Button.Left, XboxButtons.DPadLeft),
        (Xbox360Button.Right, XboxButtons.DPadRight),
    ];
}
