using Farmamia.Agent.Dominio.Modelos;

namespace Farmamia.Agent.Dominio.Puertos;

public interface IRespaldoPos
{
    Task<RespaldoPos> CrearAsync(string rutaPos, string versionActual, CancellationToken cancellationToken);

    Task RestaurarAsync(RespaldoPos respaldo, string rutaPos, CancellationToken cancellationToken);
}
