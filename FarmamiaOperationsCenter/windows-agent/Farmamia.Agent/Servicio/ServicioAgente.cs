using Farmamia.Agent.Aplicacion.CasosUso;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Microsoft.Extensions.Options;

namespace Farmamia.Agent.Servicio;

public sealed class ServicioAgente : BackgroundService
{
    private readonly InicializarAgenteCasoUso inicializarAgenteCasoUso;
    private readonly EnviarLatidoCasoUso enviarLatidoCasoUso;
    private readonly PrepararActualizacionCasoUso prepararActualizacionCasoUso;
    private readonly IColaEventosAgente colaEventosAgente;
    private readonly ILogger<ServicioAgente> logger;
    private readonly OpcionesAgente opciones;

    public ServicioAgente(
        InicializarAgenteCasoUso inicializarAgenteCasoUso,
        EnviarLatidoCasoUso enviarLatidoCasoUso,
        PrepararActualizacionCasoUso prepararActualizacionCasoUso,
        IColaEventosAgente colaEventosAgente,
        ILogger<ServicioAgente> logger,
        IOptions<OpcionesAgente> opciones
    )
    {
        this.inicializarAgenteCasoUso = inicializarAgenteCasoUso;
        this.enviarLatidoCasoUso = enviarLatidoCasoUso;
        this.prepararActualizacionCasoUso = prepararActualizacionCasoUso;
        this.colaEventosAgente = colaEventosAgente;
        this.logger = logger;
        this.opciones = opciones.Value;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        logger.LogInformation("Iniciando agente Farmamia Operations");
        await colaEventosAgente.InicializarAsync(stoppingToken);
        await colaEventosAgente.RecuperarEnviosInterrumpidosAsync(stoppingToken);

        CredencialesAgente credenciales = await InicializarConReintentosAsync(stoppingToken);

        using var temporizador = new PeriodicTimer(TimeSpan.FromSeconds(opciones.IntervaloHeartbeatSegundos));
        int fallosConsecutivos = 0;

        do
        {
            try
            {
                await enviarLatidoCasoUso.EjecutarAsync(credenciales, stoppingToken);
                logger.LogInformation("Heartbeat enviado para equipo {IdEquipo}", credenciales.IdEquipo);

                await prepararActualizacionCasoUso.EjecutarAsync(credenciales, stoppingToken);
                fallosConsecutivos = 0;
            }
            catch (Exception ex)
            {
                fallosConsecutivos++;
                TimeSpan espera = Backoff(fallosConsecutivos);
                logger.LogError(ex, "Ciclo del agente fallo. Reintento en {Espera}. Fallos consecutivos: {Fallos}", espera, fallosConsecutivos);
                await Task.Delay(espera, stoppingToken);
            }
        }
        while (await temporizador.WaitForNextTickAsync(stoppingToken));
    }

    private async Task<CredencialesAgente> InicializarConReintentosAsync(CancellationToken stoppingToken)
    {
        int intento = 0;
        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                return await inicializarAgenteCasoUso.EjecutarAsync(stoppingToken);
            }
            catch (Exception ex)
            {
                intento++;
                TimeSpan espera = Backoff(intento);
                logger.LogError(ex, "No se pudo inicializar agente. Reintento en {Espera}", espera);
                await Task.Delay(espera, stoppingToken);
            }
        }

        throw new OperationCanceledException(stoppingToken);
    }

    private TimeSpan Backoff(int intento)
    {
        int segundos = Math.Min(
            opciones.BackoffMaximoSegundos,
            opciones.BackoffInicialSegundos * (int)Math.Pow(2, Math.Max(0, intento - 1))
        );
        return TimeSpan.FromSeconds(Math.Max(1, segundos));
    }
}
