using System.Text.Json;
using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Farmamia.Agent.Infraestructura.Api;
using Farmamia.Agent.Infraestructura.Cola;
using Microsoft.Extensions.Options;

namespace Farmamia.Agent.Servicio;

public sealed class DespachadorColaEventosServicio : BackgroundService
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);

    private readonly IColaEventosAgente colaEventos;
    private readonly IConfiguracionLocalAgente configuracionLocal;
    private readonly ClienteOperacionesFarmamia clienteRemoto;
    private readonly ILogger<DespachadorColaEventosServicio> logger;
    private readonly OpcionesAgente opciones;
    private readonly Random jitter = new();
    private DateTimeOffset ultimoDiagnostico = DateTimeOffset.MinValue;

    public DespachadorColaEventosServicio(
        IColaEventosAgente colaEventos,
        IConfiguracionLocalAgente configuracionLocal,
        ClienteOperacionesFarmamia clienteRemoto,
        ILogger<DespachadorColaEventosServicio> logger,
        IOptions<OpcionesAgente> opciones
    )
    {
        this.colaEventos = colaEventos;
        this.configuracionLocal = configuracionLocal;
        this.clienteRemoto = clienteRemoto;
        this.logger = logger;
        this.opciones = opciones.Value;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        await colaEventos.InicializarAsync(stoppingToken);
        await colaEventos.RecuperarEnviosInterrumpidosAsync(stoppingToken);
        logger.LogInformation("Despachador de cola de eventos iniciado");

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                await DespacharPendientesUnaVezAsync(stoppingToken);
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "Fallo inesperado despachando cola de eventos");
            }

            await Task.Delay(TimeSpan.FromSeconds(10), stoppingToken);
        }
    }

    public async Task DespacharPendientesUnaVezAsync(CancellationToken cancellationToken)
    {
        CredencialesAgente? credenciales = await configuracionLocal.LeerCredencialesAsync(cancellationToken);
        if (credenciales is null)
        {
            await RegistrarDiagnosticoAsync(cancellationToken);
            return;
        }

        IReadOnlyList<EventoPendienteAgente> pendientes = await colaEventos.ObtenerPendientesAsync(
            25,
            DateTimeOffset.UtcNow,
            cancellationToken
        );

        foreach (EventoPendienteAgente pendiente in pendientes)
        {
            await DespacharUnoAsync(credenciales, pendiente, cancellationToken);
        }

        await RegistrarDiagnosticoAsync(cancellationToken);
    }

    private async Task DespacharUnoAsync(
        CredencialesAgente credenciales,
        EventoPendienteAgente pendiente,
        CancellationToken cancellationToken
    )
    {
        try
        {
            await colaEventos.MarcarEnviandoAsync(pendiente.Id, cancellationToken);

            if (pendiente.TipoEvento == ClienteOperacionesFarmamiaConCola.TipoResultadoActualizacion)
            {
                ResultadoActualizacionAgente resultado = Deserializar<ResultadoActualizacionAgente>(pendiente);
                await clienteRemoto.ReportarResultadoAsync(credenciales, resultado, cancellationToken);
            }
            else
            {
                EventoAgente evento = Deserializar<EventoAgente>(pendiente);
                await clienteRemoto.ReportarEventoAsync(credenciales, evento, cancellationToken);
            }

            await colaEventos.MarcarEnviadoAsync(pendiente.Id, cancellationToken);
            logger.LogInformation("Evento en cola {IdEvento} enviado correctamente", pendiente.Id);
        }
        catch (Exception ex)
        {
            DateTimeOffset siguienteIntento = DateTimeOffset.UtcNow.Add(Backoff(pendiente.CantidadIntentos + 1));
            await colaEventos.MarcarFallidoAsync(
                pendiente.Id,
                ex.Message,
                opciones.MaxIntentosColaEventos,
                siguienteIntento,
                cancellationToken
            );
            logger.LogWarning(
                ex,
                "No se pudo despachar evento en cola {IdEvento}. Intentos: {Intentos}",
                pendiente.Id,
                pendiente.CantidadIntentos + 1
            );
        }
    }

    private async Task RegistrarDiagnosticoAsync(CancellationToken cancellationToken)
    {
        DateTimeOffset ahora = DateTimeOffset.UtcNow;
        if (ultimoDiagnostico.AddMinutes(1) > ahora)
        {
            return;
        }

        ultimoDiagnostico = ahora;
        DiagnosticoColaEventosAgente diagnostico = await colaEventos.ObtenerDiagnosticoAsync(cancellationToken);
        logger.LogInformation(
            "Diagnostico cola eventos: pendientes={Pendientes}, enviando={Enviando}, enviados={Enviados}, fallidos={Fallidos}, deadLetter={DeadLetter}, pendienteMasAntiguo={PendienteMasAntiguo}",
            diagnostico.Pendientes,
            diagnostico.Enviando,
            diagnostico.Enviados,
            diagnostico.Fallidos,
            diagnostico.DeadLetter,
            diagnostico.EventoMasAntiguoPendiente
        );

        if (diagnostico.DeadLetter > 0)
        {
            logger.LogError(
                "ALERTA OPERATIVA: cola de eventos contiene {DeadLetter} eventos DEAD_LETTER. Ultimo={UltimoDeadLetterEn}. Error={UltimoError}",
                diagnostico.DeadLetter,
                diagnostico.UltimoDeadLetterEn,
                diagnostico.UltimoErrorDeadLetter
            );
        }
    }

    private T Deserializar<T>(EventoPendienteAgente pendiente)
    {
        T? valor = JsonSerializer.Deserialize<T>(pendiente.PayloadJson, JsonOptions);
        if (valor is null)
        {
            throw new InvalidOperationException("Payload local de cola invalido: " + pendiente.Id);
        }

        return valor;
    }

    private TimeSpan Backoff(int intento)
    {
        int segundosBase = Math.Min(
            opciones.BackoffMaximoSegundos,
            opciones.BackoffInicialSegundos * (int)Math.Pow(2, Math.Max(0, intento - 1))
        );
        int segundosJitter = jitter.Next(0, Math.Max(2, opciones.BackoffInicialSegundos));
        return TimeSpan.FromSeconds(Math.Max(1, segundosBase + segundosJitter));
    }
}
