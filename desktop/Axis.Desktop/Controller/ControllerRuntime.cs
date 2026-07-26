namespace Axis.Desktop.Controller;

public sealed class ControllerRuntime(IVirtualGamepadBackend backend)
{
    private readonly IVirtualGamepadBackend _backend = backend;
    private XInputReport _lastReport;

    public XInputReport LastReport => _lastReport;

    public async ValueTask ApplyAsync(NormalizedControllerState state, CancellationToken cancellationToken)
    {
        // 1. Ensure Virtual Xbox 360 Controller is connected via ViGEmBus
        if (!_backend.IsConnected)
        {
            try { await _backend.ConnectAsync(cancellationToken).ConfigureAwait(false); } catch { }
        }

        // 2. Submit Pure XInput Report to Windows Gamepad Subsystem
        var report = XInputMapper.Map(state);
        try { await _backend.SubmitAsync(report, cancellationToken).ConfigureAwait(false); } catch { }
        _lastReport = report;
    }

    public ValueTask DisconnectAsync(CancellationToken cancellationToken) => _backend.DisconnectAsync(cancellationToken);
}
