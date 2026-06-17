using System.Net.Http.Headers;
using Farmamia.Agent.Aplicacion.CasosUso;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Farmamia.Agent.Infraestructura.Sse;
using Microsoft.Extensions.Options;

namespace Farmamia.Agent.Servicio;

public sealed class ReceptorSseInstrucciones : BackgroundService
{
    private const string NombreClienteSse = "sse";
    private const string EventoInstruccionDisponible = "instruccion_disponible";
    private static readonly TimeSpan EsperaInicialCredenciales = TimeSpan.FromSeconds(5);
    private static readonly TimeSpan BackoffInicial = TimeSpan.FromSeconds(5);
    private static readonly TimeSpan BackoffMaximo = TimeSpan.FromMinutes(2);

    private readonly IConfiguracionLocalAgente configuracionLocal;
    private readonly PrepararActualizacionCasoUso prepararActualizacion;
    private readonly IHttpClientFactory httpClientFactory;
    private readonly OpcionesAgente opciones;
    private readonly ILogger<ReceptorSseInstrucciones> logger;

    public ReceptorSseInstrucciones(
        IConfiguracionLocalAgente configuracionLocal,
        PrepararActualizacionCasoUso prepararActualizacion,
        IHttpClientFactory httpClientFactory,
        IOptions<OpcionesAgente> opciones,
        ILogger<ReceptorSseInstrucciones> logger
    )
    {
        this.configuracionLocal = configuracionLocal;
        this.prepararActualizacion = prepararActualizacion;
        this.httpClientFactory = httpClientFactory;
        this.opciones = opciones.Value;
        this.logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        CredencialesAgente credenciales = await EsperarCredencialesAsync(stoppingToken);

        TimeSpan backoff = BackoffInicial;

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                await ConectarYRecibirAsync(credenciales, stoppingToken);
                backoff = BackoffInicial;
            }
            catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
            {
                break;
            }
            catch (Exception ex)
            {
                logger.LogWarning(ex, "Canal SSE desconectado. Reconectando en {Backoff}", backoff);
                await Task.Delay(backoff, stoppingToken);
                backoff = TimeSpan.FromSeconds(Math.Min(backoff.TotalSeconds * 2, BackoffMaximo.TotalSeconds));
            }
        }
    }

    private async Task ConectarYRecibirAsync(CredencialesAgente credenciales, CancellationToken cancellationToken)
    {
        HttpClient cliente = httpClientFactory.CreateClient(NombreClienteSse);

        using var solicitud = new HttpRequestMessage(
            HttpMethod.Get,
            $"/api/agent/{credenciales.IdEquipo}/notifications"
        );
        solicitud.Headers.Authorization = new AuthenticationHeaderValue("Bearer", credenciales.TokenAgente);
        solicitud.Headers.Accept.ParseAdd("text/event-stream");

        using HttpResponseMessage respuesta = await cliente.SendAsync(
            solicitud,
            HttpCompletionOption.ResponseHeadersRead,
            cancellationToken
        );
        respuesta.EnsureSuccessStatusCode();

        logger.LogInformation("Canal SSE conectado para equipo {IdEquipo}", credenciales.IdEquipo);

        await using Stream stream = await respuesta.Content.ReadAsStreamAsync(cancellationToken);

        await foreach (string _ in LectorSseLineas.LeerEventosAsync(stream, EventoInstruccionDisponible, cancellationToken))
        {
            logger.LogInformation("Push SSE recibido: instruccion_disponible para equipo {IdEquipo}", credenciales.IdEquipo);
            await prepararActualizacion.EjecutarAsync(credenciales, cancellationToken);
        }
    }

    private async Task<CredencialesAgente> EsperarCredencialesAsync(CancellationToken cancellationToken)
    {
        while (!cancellationToken.IsCancellationRequested)
        {
            CredencialesAgente? credenciales = await configuracionLocal.LeerCredencialesAsync(cancellationToken);
            if (credenciales is not null)
                return credenciales;

            await Task.Delay(EsperaInicialCredenciales, cancellationToken);
        }

        throw new OperationCanceledException(cancellationToken);
    }
}
