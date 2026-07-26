namespace Axis.Desktop.Controller;

[Flags]
public enum XboxButtons : ushort
{
    None = 0, DPadUp = 0x0001, DPadDown = 0x0002, DPadLeft = 0x0004, DPadRight = 0x0008,
    Start = 0x0010, Back = 0x0020, LeftThumb = 0x0040, RightThumb = 0x0080,
    LeftShoulder = 0x0100, RightShoulder = 0x0200, A = 0x1000, B = 0x2000, X = 0x4000, Y = 0x8000,
}

public readonly record struct NormalizedControllerState(
    float Steering, float Throttle, float Brake, float Handbrake, XboxButtons Buttons)
{
    public static readonly NormalizedControllerState Neutral = new(0, 0, 0, 0, XboxButtons.None);
}

public readonly record struct XInputReport(short LeftThumbX, byte LeftTrigger, byte RightTrigger, XboxButtons Buttons);

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
    public static XInputReport Map(in NormalizedControllerState state) => new(
        LeftThumbX: (short)MathF.Round(Math.Clamp(state.Steering, -1f, 1f) * short.MaxValue),
        LeftTrigger: ToTrigger(state.Brake),
        RightTrigger: ToTrigger(state.Throttle),
        Buttons: state.Buttons);

    private static byte ToTrigger(float value) => (byte)MathF.Round(Math.Clamp(value, 0f, 1f) * byte.MaxValue);
}

/// Fails closed until a maintained, signed Windows virtual-device provider is selected.
/// It must never report a controller connected when Windows has not registered one.
public sealed class UnavailableVirtualGamepadBackend(string reason) : IVirtualGamepadBackend
{
    public string DisplayName => "No virtual controller backend";
    public bool IsConnected => false;
    public ValueTask ConnectAsync(CancellationToken cancellationToken) => ValueTask.FromException(new InvalidOperationException(reason));
    public ValueTask SubmitAsync(XInputReport report, CancellationToken cancellationToken) => ValueTask.FromException(new InvalidOperationException(reason));
    public ValueTask DisconnectAsync(CancellationToken cancellationToken) => ValueTask.CompletedTask;
    public ValueTask DisposeAsync() => ValueTask.CompletedTask;
}
