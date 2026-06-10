using System.IO.Compression;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;

namespace Farmamia.Agent.Infraestructura.Actualizacion;

public sealed class ActualizadorPosZip : IActualizadorPos
{
    private const string EjecutablePos = "Zabyca.Pos.Desktop.exe";

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

    public bool Validar(string rutaPos)
    {
        return File.Exists(Path.Combine(rutaPos, EjecutablePos));
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
