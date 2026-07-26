using System.ComponentModel;
using System.Net;
using System.Net.Sockets;
using System.Runtime.CompilerServices;
using Axis.Desktop.Networking;

namespace Axis.Desktop.ViewModels;

public sealed class MainViewModel : INotifyPropertyChanged
{
    private string _lastDiscovery = "No phone has discovered this PC yet.";
    public string DiscoveryStatus { get; } = "Ready for Axis discovery";
    public string LanAddress { get; } = "LAN address: " + FindLanAddress();
    public string LastDiscovery { get => _lastDiscovery; private set { _lastDiscovery = value; OnPropertyChanged(); } }
    public event PropertyChangedEventHandler? PropertyChanged;

    public MainViewModel()
    {
        var responder = new DiscoveryResponder(message => LastDiscovery = message);
        _ = Task.Run(responder.RunAsync);
    }
    private static string FindLanAddress() => Dns.GetHostEntry(Dns.GetHostName()).AddressList.FirstOrDefault(address => address.AddressFamily == AddressFamily.InterNetwork && !IPAddress.IsLoopback(address))?.ToString() ?? "No IPv4 address available";
    private void OnPropertyChanged([CallerMemberName] string? name = null) => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
