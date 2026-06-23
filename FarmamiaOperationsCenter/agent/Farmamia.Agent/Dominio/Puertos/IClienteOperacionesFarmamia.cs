using Farmamia.Agent.Dominio.Modelos;

namespace Farmamia.Agent.Dominio.Puertos;

public interface IClienteOperacionesFarmamia
{
    Task<CredencialesAgente> RegistrarAsync(string codigoSucursal, DatosInventarioEquipo inventario, CancellationToken cancellationToken);

    Task EnviarLatidoAsync(CredencialesAgente credenciales, LatidoAgente latido, CancellationToken cancellationToken);

    Task<InstruccionActualizacion?> ConsultarInstruccionAsync(CredencialesAgente credenciales, CancellationToken cancellationToken);

    Task<Stream> DescargarPaqueteAsync(CredencialesAgente credenciales, string urlDescarga, Guid idObjetivoDespliegue, CancellationToken cancellationToken);

    Task ReportarEventoAsync(CredencialesAgente credenciales, EventoAgente evento, CancellationToken cancellationToken);

    Task ReportarResultadoAsync(CredencialesAgente credenciales, ResultadoActualizacionAgente resultado, CancellationToken cancellationToken);
}
