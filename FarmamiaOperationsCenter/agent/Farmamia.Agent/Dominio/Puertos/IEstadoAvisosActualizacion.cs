namespace Farmamia.Agent.Dominio.Puertos;

public interface IEstadoAvisosActualizacion
{
    Task<bool> FueEnviadoAsync(Guid idObjetivoDespliegue, TimeOnly horaAviso, CancellationToken cancellationToken);

    Task RegistrarEnviadoAsync(Guid idObjetivoDespliegue, TimeOnly horaAviso, CancellationToken cancellationToken);
}
