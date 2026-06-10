namespace Farmamia.Agent.Dominio.Puertos;

public interface IProcesoPos
{
    Task<bool> CerrarSiEjecutandoseAsync(CancellationToken cancellationToken);

    Task IniciarAsync(string rutaPos, CancellationToken cancellationToken);
}
