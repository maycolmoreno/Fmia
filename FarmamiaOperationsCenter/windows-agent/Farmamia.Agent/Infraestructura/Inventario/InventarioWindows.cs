using System.Diagnostics;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Microsoft.Extensions.Options;

namespace Farmamia.Agent.Infraestructura.Inventario;

public sealed class InventarioWindows : IInventarioEquipo
{
    private const string NombreProcesoPos = "Zabyca.Pos.Desktop";

    private readonly OpcionesAgente opciones;

    public InventarioWindows(IOptions<OpcionesAgente> opciones)
    {
        this.opciones = opciones.Value;
    }

    public DatosInventarioEquipo ObtenerInventario()
    {
        DriveInfo disco = new(Path.GetPathRoot(opciones.RutaPos) ?? "C:\\");

        return new DatosInventarioEquipo(
            Environment.MachineName,
            ObtenerDireccionIp(),
            ObtenerDireccionMac(),
            RuntimeInformation.OSDescription,
            opciones.VersionAgente,
            ObtenerVersionPos(),
            opciones.RutaPos,
            disco.AvailableFreeSpace / 1024 / 1024,
            disco.TotalSize / 1024 / 1024,
            Process.GetProcessesByName(NombreProcesoPos).Length > 0
        );
    }

    private string ObtenerVersionPos()
    {
        string ejecutable = Path.Combine(opciones.RutaPos, "Zabyca.Pos.Desktop.exe");
        if (!File.Exists(ejecutable))
        {
            return "UNKNOWN";
        }

        FileVersionInfo version = FileVersionInfo.GetVersionInfo(ejecutable);
        return version.ProductVersion ?? version.FileVersion ?? "UNKNOWN";
    }

    private static string ObtenerDireccionIp()
    {
        return NetworkInterface.GetAllNetworkInterfaces()
            .Where(interfaz => interfaz.OperationalStatus == OperationalStatus.Up)
            .SelectMany(interfaz => interfaz.GetIPProperties().UnicastAddresses)
            .Where(direccion => direccion.Address.AddressFamily == AddressFamily.InterNetwork)
            .Select(direccion => direccion.Address.ToString())
            .FirstOrDefault() ?? "0.0.0.0";
    }

    private static string ObtenerDireccionMac()
    {
        return NetworkInterface.GetAllNetworkInterfaces()
            .Where(interfaz => interfaz.OperationalStatus == OperationalStatus.Up)
            .Select(interfaz => interfaz.GetPhysicalAddress().ToString())
            .FirstOrDefault(mac => !string.IsNullOrWhiteSpace(mac)) ?? "UNKNOWN";
    }
}
