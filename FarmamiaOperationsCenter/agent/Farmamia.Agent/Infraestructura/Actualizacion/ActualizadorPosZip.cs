using System.Diagnostics;
using System.IO.Compression;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Microsoft.Extensions.Options;

namespace Farmamia.Agent.Infraestructura.Actualizacion;

public sealed class ActualizadorPosZip : IActualizadorPos
{
    private const string EjecutablePos = "Zabyca.Pos.Desktop.exe";
    private const string MetodoSmokeTest = "SMOKE_TEST";
    private const string MetodoProcesoVivo = "PROCESO_VIVO_20S";
    private const string MetodoEjecutableNoEncontrado = "EJECUTABLE_NO_ENCONTRADO";

    private readonly OpcionesAgente opciones;

    public ActualizadorPosZip(IOptions<OpcionesAgente> opciones)
    {
        this.opciones = opciones.Value;
    }

    public Task AplicarAsync(ArchivoPaqueteLocal paquete, string rutaPos, CancellationToken cancellationToken)
    {
        if (!File.Exists(paquete.RutaArchivo))
        {
            throw new FileNotFoundException("Paquete POS no encontrado", paquete.RutaArchivo);
        }

        string temporal = Path.Combine(Path.GetTempPath(), "FarmamiaOps", "Update", Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(temporal);

        try
        {
            ZipFile.ExtractToDirectory(paquete.RutaArchivo, temporal, overwriteFiles: true);
            if (!File.Exists(Path.Combine(temporal, EjecutablePos)))
            {
                throw new InvalidOperationException("El paquete POS no contiene " + EjecutablePos);
            }

            Directory.CreateDirectory(rutaPos);
            CopiarDirectorio(temporal, rutaPos, cancellationToken);
        }
        finally
        {
            if (Directory.Exists(temporal))
            {
                Directory.Delete(temporal, recursive: true);
            }
        }

        return Task.CompletedTask;
    }

    public async Task<ResultadoValidacionPos> ValidarAsync(string rutaPos, CancellationToken cancellationToken)
    {
        string ejecutable = Path.Combine(rutaPos, EjecutablePos);
        if (!File.Exists(ejecutable))
        {
            return ResultadoValidacionPos.Fallo(
                MetodoEjecutableNoEncontrado,
                $"No se encontro {EjecutablePos} en {rutaPos} despues de aplicar el paquete"
            );
        }

        ResultadoValidacionPos? resultadoSmokeTest = await IntentarSmokeTestAsync(ejecutable, rutaPos, cancellationToken);
        if (resultadoSmokeTest is not null)
        {
            return resultadoSmokeTest;
        }

        // La version instalada no reconoce "--smoke-test" (no termino dentro del tiempo esperado):
        // se cae al metodo de compatibilidad para versiones antiguas del POS.
        return await ValidarProcesoVivoAsync(ejecutable, rutaPos, cancellationToken);
    }

    private async Task<ResultadoValidacionPos?> IntentarSmokeTestAsync(string ejecutable, string rutaPos, CancellationToken cancellationToken)
    {
        Process? proceso;
        try
        {
            proceso = Process.Start(new ProcessStartInfo
            {
                FileName = ejecutable,
                Arguments = "--smoke-test",
                WorkingDirectory = rutaPos,
                UseShellExecute = false,
                CreateNoWindow = true
            });
        }
        catch (Exception)
        {
            // El ejecutable no pudo arrancar con el flag (p.ej. no es un binario Win32 valido en este entorno);
            // se trata igual que "smoke-test no disponible" y se intenta el metodo de respaldo.
            return null;
        }

        if (proceso is null)
        {
            return null;
        }

        using (proceso)
        {
            using CancellationTokenSource limiteTiempo = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
            limiteTiempo.CancelAfter(TimeSpan.FromSeconds(opciones.SmokeTestTimeoutSegundos));

            try
            {
                await proceso.WaitForExitAsync(limiteTiempo.Token);
            }
            catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested)
            {
                if (!proceso.HasExited)
                {
                    proceso.Kill(entireProcessTree: true);
                }
                return null;
            }

            return proceso.ExitCode == 0
                ? ResultadoValidacionPos.Exito(MetodoSmokeTest)
                : ResultadoValidacionPos.Fallo(
                    MetodoSmokeTest,
                    $"{EjecutablePos} --smoke-test devolvio codigo de salida {proceso.ExitCode}"
                );
        }
    }

    private async Task<ResultadoValidacionPos> ValidarProcesoVivoAsync(string ejecutable, string rutaPos, CancellationToken cancellationToken)
    {
        Process? proceso;
        try
        {
            proceso = Process.Start(new ProcessStartInfo
            {
                FileName = ejecutable,
                WorkingDirectory = rutaPos,
                UseShellExecute = true
            });
        }
        catch (Exception ex)
        {
            return ResultadoValidacionPos.Fallo(MetodoProcesoVivo, "No se pudo iniciar " + EjecutablePos + ": " + ex.Message);
        }

        if (proceso is null)
        {
            return ResultadoValidacionPos.Fallo(MetodoProcesoVivo, "No se pudo iniciar " + EjecutablePos);
        }

        using (proceso)
        {
            await Task.Delay(TimeSpan.FromSeconds(opciones.ValidacionProcesoVivoSegundos), cancellationToken);

            if (proceso.HasExited)
            {
                return ResultadoValidacionPos.Fallo(
                    MetodoProcesoVivo,
                    $"{EjecutablePos} termino antes de {opciones.ValidacionProcesoVivoSegundos}s (codigo de salida {proceso.ExitCode})"
                );
            }

            // Este proceso fue lanzado solo para comprobar que el POS sigue vivo; se cierra aqui
            // para que, si corresponde, PrepararActualizacionCasoUso reabra una unica instancia
            // visible para el usuario despues de reportar el resultado.
            CerrarProcesoDeValidacion(proceso);
            return ResultadoValidacionPos.Exito(MetodoProcesoVivo);
        }
    }

    private static void CerrarProcesoDeValidacion(Process proceso)
    {
        try
        {
            if (proceso.CloseMainWindow())
            {
                proceso.WaitForExit(5000);
            }

            if (!proceso.HasExited)
            {
                proceso.Kill(entireProcessTree: true);
            }
        }
        catch (InvalidOperationException)
        {
            // El proceso ya termino entre la comprobacion y el cierre; nada que hacer.
        }
    }

    private static void CopiarDirectorio(string origen, string destino, CancellationToken cancellationToken)
    {
        foreach (string directorio in Directory.EnumerateDirectories(origen, "*", SearchOption.AllDirectories))
        {
            cancellationToken.ThrowIfCancellationRequested();
            string relativo = Path.GetRelativePath(origen, directorio);
            Directory.CreateDirectory(Path.Combine(destino, relativo));
        }

        foreach (string archivo in Directory.EnumerateFiles(origen, "*", SearchOption.AllDirectories))
        {
            cancellationToken.ThrowIfCancellationRequested();
            string relativo = Path.GetRelativePath(origen, archivo);
            string archivoDestino = Path.Combine(destino, relativo);
            Directory.CreateDirectory(Path.GetDirectoryName(archivoDestino)!);
            File.Copy(archivo, archivoDestino, overwrite: true);
        }
    }
}
