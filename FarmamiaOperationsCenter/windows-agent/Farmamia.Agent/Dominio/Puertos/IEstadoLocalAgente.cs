using Farmamia.Agent.Dominio.Modelos;

namespace Farmamia.Agent.Dominio.Puertos;

public interface IEstadoLocalAgente
{
    Task<EstadoLocalAgente?> LeerAsync(CancellationToken cancellationToken);

    Task GuardarAsync(EstadoLocalAgente estado, CancellationToken cancellationToken);
}
