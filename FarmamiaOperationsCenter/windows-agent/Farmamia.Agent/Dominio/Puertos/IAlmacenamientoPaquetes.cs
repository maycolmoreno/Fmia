using Farmamia.Agent.Dominio.Modelos;

namespace Farmamia.Agent.Dominio.Puertos;

public interface IAlmacenamientoPaquetes
{
    Task<ArchivoPaqueteLocal> GuardarAsync(Guid idPaquete, string version, Stream contenido, CancellationToken cancellationToken);
}
