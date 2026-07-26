using System.Collections.Concurrent;
using System.Net;
using Nefarius.ViGEm.Client;
using Nefarius.ViGEm.Client.Targets;

namespace Axis.Desktop.Controller;

public sealed class MultiControllerManager : IAsyncDisposable
{
    private readonly ConcurrentDictionary<IPAddress, (int playerIndex, ControllerRuntime runtime)> _activeRuntimes = new();
    private readonly List<ControllerRuntime> _runtimes = new();
    private readonly ViGEmClient? _client;
    private readonly List<IXbox360Controller> _controllers = new();

    public int ActiveControllerCount => _activeRuntimes.Count;

    public MultiControllerManager()
    {
        if (OperatingSystem.IsWindows())
        {
            try
            {
                _client = new ViGEmClient();
                for (int i = 0; i < 4; i++)
                {
                    var ctrl = _client.CreateXbox360Controller();
                    ctrl.Connect();
                    _controllers.Add(ctrl);
                    var backend = new ViGEmInstanceBackend(ctrl);
                    _runtimes.Add(new ControllerRuntime(backend));
                }
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"ViGEm multi-controller init error: {ex.Message}");
            }
        }
    }

    public (int playerIndex, ControllerRuntime runtime) GetRuntimeForEndpoint(IPEndPoint endpoint)
    {
        return _activeRuntimes.GetOrAdd(endpoint.Address, ip =>
        {
            int index = _activeRuntimes.Count % Math.Max(1, _runtimes.Count);
            var runtime = _runtimes.Count > 0 ? _runtimes[index] : new ControllerRuntime(new UnavailableVirtualGamepadBackend("ViGEm unavailable"));
            return (index + 1, runtime);
        });
    }

    public async ValueTask DisposeAsync()
    {
        foreach (var ctrl in _controllers)
        {
            try { ctrl.Disconnect(); } catch { }
        }
        _controllers.Clear();
        _client?.Dispose();
        await Task.CompletedTask;
    }
}
