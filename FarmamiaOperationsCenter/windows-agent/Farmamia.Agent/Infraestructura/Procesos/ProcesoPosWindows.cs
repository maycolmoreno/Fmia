using System.Diagnostics;
using Farmamia.Agent.Dominio.Puertos;

namespace Farmamia.Agent.Infraestructura.Procesos;

public sealed class ProcesoPosWindows : IProcesoPos
{
    private const string NombreProcesoPos = "Zabyca.Pos.Desktop";
    private const string EjecutablePos = "Zabyca.Pos.Desktop.exe";

    public async Task<bool> CerrarSiEjecutandoseAsync(CancellationToken cancellationToken)
    {
        Process[] procesos = Process.GetProcessesByName(NombreProcesoPos);
        if (procesos.Length == 0)
        {
            return false;
        }

        foreach (Process proceso in procesos)
        {
            cancellationToken.ThrowIfCancellationRequested();

            using (proceso)
            {
                if (proceso.CloseMainWindow())
                {
                    Task espera = proceso.WaitForExitAsync(cancellationToken);
                    Task timeout = Task.Delay(TimeSpan.FromSeconds(20), cancellationToken);
                    await Task.WhenAny(espera, timeout);
                }

                if (!proceso.HasExited)
                {
                    proceso.Kill(entireProcessTree: true);
                    await proceso.WaitForExitAsync(cancellationToken);
                }
            }
        }

        return true;
    }

    public Task IniciarAsync(string rutaPos, CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();
        string ejecutable = Path.Combine(rutaPos, EjecutablePos);
        if (!File.Exists(ejecutable))
        {
            throw new FileNotFoundException("Ejecutable POS no encontrado", ejecutable);
        }

        Process.Start(new ProcessStartInfo
        {
            FileName = ejecutable,
            WorkingDirectory = rutaPos,
            UseShellExecute = true
        });

        return Task.CompletedTask;
    }
}
