namespace Axis.Desktop.Controller;

[Flags]
public enum XboxButtons : ushort
{
    None = 0,
    DPadUp = 0x0001,
    DPadDown = 0x0002,
    DPadLeft = 0x0004,
    DPadRight = 0x0008,
    Start = 0x0010,
    Back = 0x0020,
    LeftThumb = 0x0040,   // L3
    RightThumb = 0x0080,  // R3
    LeftShoulder = 0x0100,// LB
    RightShoulder = 0x0200,// RB
    A = 0x1000,
    B = 0x2000,
    X = 0x4000,
    Y = 0x8000,
}

public readonly record struct NormalizedControllerState(
    float LeftStickX, float LeftStickY,
    float RightStickX, float RightStickY,
    float Throttle, float Brake, float Handbrake,
    XboxButtons Buttons)
{
    public static readonly NormalizedControllerState Neutral = new(0, 0, 0, 0, 0, 0, 0, XboxButtons.None);
}

public readonly record struct XInputReport(
    short LeftThumbX, short LeftThumbY,
    short RightThumbX, short RightThumbY,
    byte LeftTrigger, byte RightTrigger,
    XboxButtons Buttons);

public interface IVirtualGamepadBackend : IAsyncDisposable
{
    string DisplayName { get; }
    bool IsConnected { get; }
    ValueTask ConnectAsync(CancellationToken cancellationToken);
    ValueTask SubmitAsync(XInputReport report, CancellationToken cancellationToken);
    ValueTask DisconnectAsync(CancellationToken cancellationToken);
}

public static class XInputMapper
{
    public static XInputReport Map(in NormalizedControllerState state)
    {
        byte lt = ToTrigger(MathF.Max(state.Brake, state.Handbrake));
        byte rt = ToTrigger(state.Throttle);

        return new XInputReport(
            LeftThumbX: ToAxis(state.LeftStickX),
            LeftThumbY: ToAxis(state.LeftStickY),
            RightThumbX: ToAxis(state.RightStickX),
            RightThumbY: ToAxis(state.RightStickY),
            LeftTrigger: lt,
            RightTrigger: rt,
            Buttons: state.Buttons);
    }

    private static short ToAxis(float value) => (short)MathF.Round(Math.Clamp(value, -1f, 1f) * short.MaxValue);
    private static byte ToTrigger(float value) => (byte)MathF.Round(Math.Clamp(value, 0f, 1f) * byte.MaxValue);
}

public sealed class UnavailableVirtualGamepadBackend(string reason) : IVirtualGamepadBackend
{
    public string DisplayName => "No virtual controller backend";
    public bool IsConnected => false;
    public ValueTask ConnectAsync(CancellationToken cancellationToken) => ValueTask.FromException(new InvalidOperationException(reason));
    public ValueTask SubmitAsync(XInputReport report, CancellationToken cancellationToken) => ValueTask.FromException(new InvalidOperationException(reason));
    public ValueTask DisconnectAsync(CancellationToken cancellationToken) => ValueTask.CompletedTask;
    public ValueTask DisposeAsync() => ValueTask.CompletedTask;
}
