using Farmamia.Agent.Dominio.Modelos;

namespace Farmamia.Agent.Dominio.Puertos;

public interface IAvisadorUsuario
{
    Task EnviarAsync(AvisoUsuario aviso, CancellationToken cancellationToken);
}
