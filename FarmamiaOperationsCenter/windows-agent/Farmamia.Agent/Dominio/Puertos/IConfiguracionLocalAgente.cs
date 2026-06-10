using Farmamia.Agent.Dominio.Modelos;

namespace Farmamia.Agent.Dominio.Puertos;

public interface IConfiguracionLocalAgente
{
    Task PrepararEstructuraAsync(CancellationToken cancellationToken);

    Task<CredencialesAgente?> LeerCredencialesAsync(CancellationToken cancellationToken);

    Task GuardarCredencialesAsync(CredencialesAgente credenciales, CancellationToken cancellationToken);
}
