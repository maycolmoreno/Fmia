using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Microsoft.Extensions.Options;

namespace Farmamia.Agent.Infraestructura.Actualizacion;

public sealed class RespaldoPosLocal : IRespaldoPos
{
    private readonly OpcionesAgente opciones;

    public RespaldoPosLocal(IOptions<OpcionesAgente> opciones)
    {
        this.opciones = opciones.Value;
    }

    public Task<RespaldoPos> CrearAsync(string rutaPos, string versionActual, CancellationToken cancellationToken)
    {
        if (!Directory.Exists(rutaPos))
        {
            throw new DirectoryNotFoundException("Ruta POS no encontrada: " + rutaPos);
        }

        string raizRespaldos = RutaRespaldos();
        Directory.CreateDirectory(raizRespaldos);
        string nombre = $"{Sanitizar(versionActual)}-{DateTimeOffset.Now:yyyyMMddHHmmss}";
        string destino = Path.Combine(raizRespaldos, nombre);

        CopiarDirectorio(rutaPos, destino, cancellationToken, sobrescribir: true);
        LimpiarRespaldosAntiguos();

        return Task.FromResult(new RespaldoPos(destino));
    }

    public Task RestaurarAsync(RespaldoPos respaldo, string rutaPos, CancellationToken cancellationToken)
    {
        if (!Directory.Exists(respaldo.RutaRespaldo))
        {
            throw new DirectoryNotFoundException("Respaldo POS no encontrado: " + respaldo.RutaRespaldo);
        }

        Directory.CreateDirectory(rutaPos);
        CopiarDirectorio(respaldo.RutaRespaldo, rutaPos, cancellationToken, sobrescribir: true);
        return Task.CompletedTask;
    }

    private static void CopiarDirectorio(
        string origen,
        string destino,
        CancellationToken cancellationToken,
        bool sobrescribir
    )
    {
        Directory.CreateDirectory(destino);

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
            File.Copy(archivo, archivoDestino, sobrescribir);
        }
    }

    private void LimpiarRespaldosAntiguos()
    {
        DirectoryInfo raiz = new(RutaRespaldos());
        DirectoryInfo[] respaldos = raiz.GetDirectories()
            .OrderByDescending(directorio => directorio.CreationTimeUtc)
            .Skip(3)
            .ToArray();

        foreach (DirectoryInfo respaldo in respaldos)
        {
            respaldo.Delete(recursive: true);
        }
    }

    private static string Sanitizar(string valor)
    {
        char[] invalidos = Path.GetInvalidFileNameChars();
        return string.Concat(valor.Select(caracter => invalidos.Contains(caracter) ? '_' : caracter));
    }

    private string RutaRespaldos()
    {
        return Path.Combine(opciones.RutaAgente, "Backups");
    }
}
