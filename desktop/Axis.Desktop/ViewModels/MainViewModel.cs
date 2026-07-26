using System.ComponentModel;
using System.Net;
using System.Net.Sockets;
using System.Runtime.CompilerServices;
using Axis.Desktop.Controller;
using Axis.Desktop.Networking;
using Axis.Desktop.Pairing;
using Axis.Desktop.Storage;

namespace Axis.Desktop.ViewModels;

public sealed class MainViewModel : INotifyPropertyChanged
{
    private string _lastDiscovery = "No phone has discovered this PC yet.";
    private string _pairingStatus = "PIN: Waiting for phone pairing...";
    private string _connectionStatus = "Encrypted session: Disconnected";

    public string DiscoveryStatus { get; } = "Ready for Axis discovery";
    public string LanAddress { get; } = "LAN address: " + FindLanAddress();
    public string LastDiscovery { get => _lastDiscovery; private set { _lastDiscovery = value; OnPropertyChanged(); } }
    public string PairingStatus { get => _pairingStatus; private set { _pairingStatus = value; OnPropertyChanged(); } }
    public string ConnectionStatus { get => _connectionStatus; private set { _connectionStatus = value; OnPropertyChanged(); } }

    public event PropertyChangedEventHandler? PropertyChanged;

    public MainViewModel()
    {
        var responder = new DiscoveryResponder(message => LastDiscovery = message);
        _ = Task.Run(responder.RunAsync);

        IVirtualGamepadBackend backend = OperatingSystem.IsWindows()
            ? new ViGEmXbox360Backend()
            : new UnavailableVirtualGamepadBackend("Virtual controller requires Windows with ViGEmBus driver.");

        var runtime = new ControllerRuntime(backend);
        var controlReceiver = new ControlReceiver(runtime, status => ConnectionStatus = status);
        _ = Task.Run(controlReceiver.RunAsync);

        var pairingServer = new PairingServer(
            onCode: code => PairingStatus = $"PIN Code: {code}",
            onSession: key =>
            {
                controlReceiver.SessionKey = key;
                SessionStore.SaveSessionKey(key);
                ConnectionStatus = "Encrypted AES-256-GCM Session Active (120 Hz)";
            }
        );
        _ = Task.Run(pairingServer.RunAsync);
    }

    private static string FindLanAddress() =>
        Dns.GetHostEntry(Dns.GetHostName()).AddressList.FirstOrDefault(address => address.AddressFamily == AddressFamily.InterNetwork && !IPAddress.IsLoopback(address))?.ToString() ?? "No IPv4 address available";

    private void OnPropertyChanged([CallerMemberName] string? name = null) => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
