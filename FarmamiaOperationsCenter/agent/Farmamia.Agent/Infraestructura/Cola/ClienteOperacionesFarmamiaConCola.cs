using System.Text.Json;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Farmamia.Agent.Infraestructura.Api;

namespace Farmamia.Agent.Infraestructura.Cola;

public sealed class ClienteOperacionesFarmamiaConCola : IClienteOperacionesFarmamia
{
    public const string TipoResultadoActualizacion = "UPDATE_RESULT";

    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    private readonly ClienteOperacionesFarmamia clienteRemoto;
    private readonly IColaEventosAgente colaEventos;
    private readonly ILogger<ClienteOperacionesFarmamiaConCola> logger;

    public ClienteOperacionesFarmamiaConCola(
        ClienteOperacionesFarmamia clienteRemoto,
        IColaEventosAgente colaEventos,
        ILogger<ClienteOperacionesFarmamiaConCola> logger
    )
    {
        this.clienteRemoto = clienteRemoto;
        this.colaEventos = colaEventos;
        this.logger = logger;
    }

    public Task<CredencialesAgente> RegistrarAsync(
        string codigoSucursal,
        DatosInventarioEquipo inventario,
        CancellationToken cancellationToken
    )
    {
        return clienteRemoto.RegistrarAsync(codigoSucursal, inventario, cancellationToken);
    }

    public Task EnviarLatidoAsync(
        CredencialesAgente credenciales,
        LatidoAgente latido,
        CancellationToken cancellationToken
    )
    {
        return clienteRemoto.EnviarLatidoAsync(credenciales, latido, cancellationToken);
    }

    public Task<InstruccionActualizacion?> ConsultarInstruccionAsync(
        CredencialesAgente credenciales,
        CancellationToken cancellationToken
    )
    {
        return clienteRemoto.ConsultarInstruccionAsync(credenciales, cancellationToken);
    }

    public Task<Stream> DescargarPaqueteAsync(
        CredencialesAgente credenciales,
        string urlDescarga,
        Guid idObjetivoDespliegue,
        CancellationToken cancellationToken
    )
    {
        return clienteRemoto.DescargarPaqueteAsync(credenciales, urlDescarga, idObjetivoDespliegue, cancellationToken);
    }

    public async Task ReportarEventoAsync(
        CredencialesAgente credenciales,
        EventoAgente evento,
        CancellationToken cancellationToken
    )
    {
        await colaEventos.EncolarAsync(
            evento.TipoEvento,
            JsonSerializer.Serialize(evento, JsonOptions),
            evento.IdempotencyKey,
            cancellationToken
        );
        logger.LogInformation("Evento {TipoEvento} encolado con idempotencyKey {IdempotencyKey}", evento.TipoEvento, evento.IdempotencyKey);
    }

    public async Task ReportarResultadoAsync(
        CredencialesAgente credenciales,
        ResultadoActualizacionAgente resultado,
        CancellationToken cancellationToken
    )
    {
        await colaEventos.EncolarAsync(
            TipoResultadoActualizacion,
            JsonSerializer.Serialize(resultado, JsonOptions),
            resultado.IdempotencyKey,
            cancellationToken
        );
        logger.LogInformation("Resultado {Estado} encolado con idempotencyKey {IdempotencyKey}", resultado.Estado, resultado.IdempotencyKey);
    }
}
