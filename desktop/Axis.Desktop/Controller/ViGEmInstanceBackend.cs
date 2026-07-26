using Nefarius.ViGEm.Client.Targets;
using Nefarius.ViGEm.Client.Targets.Xbox360;

namespace Axis.Desktop.Controller;

public sealed class ViGEmInstanceBackend(IXbox360Controller controller) : IVirtualGamepadBackend
{
    public string DisplayName => "ViGEm Xbox 360 Controller";
    public bool IsConnected => true;

    public ValueTask ConnectAsync(CancellationToken cancellationToken) => ValueTask.CompletedTask;

    public ValueTask SubmitAsync(XInputReport report, CancellationToken cancellationToken)
    {
        try
        {
            controller.SetAxisValue(Xbox360Axis.LeftThumbX, report.LeftThumbX);
            controller.SetAxisValue(Xbox360Axis.LeftThumbY, report.LeftThumbY);
            controller.SetAxisValue(Xbox360Axis.RightThumbX, report.RightThumbX);
            controller.SetAxisValue(Xbox360Axis.RightThumbY, report.RightThumbY);

            controller.SetSliderValue(Xbox360Slider.LeftTrigger, report.LeftTrigger);
            controller.SetSliderValue(Xbox360Slider.RightTrigger, report.RightTrigger);

            foreach (var mapping in ButtonMap)
                controller.SetButtonState(mapping.button, (report.Buttons & mapping.source) != 0);

            controller.SubmitReport();
        }
        catch { }
        return ValueTask.CompletedTask;
    }

    public ValueTask DisconnectAsync(CancellationToken cancellationToken) => ValueTask.CompletedTask;
    public ValueTask DisposeAsync() => ValueTask.CompletedTask;

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
