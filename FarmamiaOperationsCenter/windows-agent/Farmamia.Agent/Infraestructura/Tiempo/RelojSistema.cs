using Farmamia.Agent.Dominio.Puertos;

namespace Farmamia.Agent.Infraestructura.Tiempo;

public sealed class RelojSistema : IRelojSistema
{
    public DateTimeOffset Ahora()
    {
        return DateTimeOffset.Now;
    }
}
