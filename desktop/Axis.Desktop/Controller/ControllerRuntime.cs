namespace Axis.Desktop.Controller;

public sealed class ControllerRuntime(IVirtualGamepadBackend backend)
{
    private readonly IVirtualGamepadBackend _backend = backend;
    private XInputReport _lastReport;

    public XInputReport LastReport => _lastReport;
    public async ValueTask ApplyAsync(NormalizedControllerState state, CancellationToken cancellationToken)
    {
        if (!_backend.IsConnected) await _backend.ConnectAsync(cancellationToken).ConfigureAwait(false);
        var report = XInputMapper.Map(state);
        await _backend.SubmitAsync(report, cancellationToken).ConfigureAwait(false);
        _lastReport = report;
    }

    public ValueTask DisconnectAsync(CancellationToken cancellationToken) => _backend.DisconnectAsync(cancellationToken);
}
