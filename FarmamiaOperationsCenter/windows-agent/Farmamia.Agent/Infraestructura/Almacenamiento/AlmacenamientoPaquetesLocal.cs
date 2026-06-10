using System.Security.Cryptography;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Microsoft.Extensions.Options;

namespace Farmamia.Agent.Infraestructura.Almacenamiento;

public sealed class AlmacenamientoPaquetesLocal : IAlmacenamientoPaquetes
{
    private readonly OpcionesAgente opciones;

    public AlmacenamientoPaquetesLocal(IOptions<OpcionesAgente> opciones)
    {
        this.opciones = opciones.Value;
    }

    public async Task<ArchivoPaqueteLocal> GuardarAsync(
        Guid idPaquete,
        string version,
        Stream contenido,
        CancellationToken cancellationToken
    )
    {
        string raiz = Path.Combine(opciones.RutaAgente, "Downloads");
        Directory.CreateDirectory(raiz);
        string nombreArchivo = $"{version}-{idPaquete}.zip";
        string ruta = Path.Combine(raiz, nombreArchivo);

        await using FileStream destino = File.Create(ruta);
        using var sha256 = SHA256.Create();
        await using var hashStream = new CryptoStream(destino, sha256, CryptoStreamMode.Write);
        await contenido.CopyToAsync(hashStream, cancellationToken);
        await hashStream.FlushAsync(cancellationToken);
        hashStream.FlushFinalBlock();

        string checksum = Convert.ToHexString(sha256.Hash ?? []).ToLowerInvariant();
        long tamanoBytes = new FileInfo(ruta).Length;

        return new ArchivoPaqueteLocal(ruta, tamanoBytes, checksum);
    }
}
