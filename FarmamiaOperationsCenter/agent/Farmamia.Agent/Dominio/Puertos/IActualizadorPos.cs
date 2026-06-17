using Farmamia.Agent.Dominio.Modelos;

namespace Farmamia.Agent.Dominio.Puertos;

public interface IActualizadorPos
{
    Task AplicarAsync(ArchivoPaqueteLocal paquete, string rutaPos, CancellationToken cancellationToken);

    bool Validar(string rutaPos);
}
