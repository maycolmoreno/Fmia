using Farmamia.Agent.Dominio.Modelos;

namespace Farmamia.Agent.Dominio.Puertos;

public interface IActualizadorPos
{
    Task AplicarAsync(ArchivoPaqueteLocal paquete, string rutaPos, CancellationToken cancellationToken);

    Task<ResultadoValidacionPos> ValidarAsync(string rutaPos, CancellationToken cancellationToken);
}
