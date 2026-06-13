using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Dominio.Puertos;
using Farmamia.Agent.Infraestructura.Api;
using Farmamia.Agent.Infraestructura.Cola;
using Farmamia.Agent.Servicio;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;
using System.Net;
using Xunit;

namespace Farmamia.Agent.Tests;

public sealed class ColaEventosAgenteSqliteTests : IDisposable
{
    private readonly string rutaTemporal = Path.Combine(Path.GetTempPath(), "farmamia-agent-queue-tests", Guid.NewGuid().ToString("N"));

    [Fact]
    public async Task EncolarAsyncGuardaEventoPendienteDurable()
    {
        ColaEventosAgenteSqlite cola = CrearCola();
        await cola.InicializarAsync(CancellationToken.None);

        await cola.EncolarAsync("UPDATE_STARTED", "{\"ok\":true}", "evt-1", CancellationToken.None);

        IReadOnlyList<EventoPendienteAgente> pendientes = await cola.ObtenerPendientesAsync(
            10,
            DateTimeOffset.UtcNow.AddSeconds(1),
            CancellationToken.None
        );

        Assert.Single(pendientes);
        Assert.Equal("evt-1", pendientes[0].IdempotencyKey);
        Assert.Equal("PENDING", pendientes[0].Estado);
    }

    [Fact]
    public async Task RecuperarEnviosInterrumpidosRegresaSendingAPending()
    {
        ColaEventosAgenteSqlite cola = CrearCola();
        await cola.InicializarAsync(CancellationToken.None);
        EventoPendienteAgente evento = await cola.EncolarAsync("UPDATE_COMPLETED", "{\"ok\":true}", "evt-2", CancellationToken.None);

        await cola.MarcarEnviandoAsync(evento.Id, CancellationToken.None);
        Assert.Empty(await cola.ObtenerPendientesAsync(10, DateTimeOffset.UtcNow.AddSeconds(1), CancellationToken.None));

        await cola.RecuperarEnviosInterrumpidosAsync(CancellationToken.None);

        IReadOnlyList<EventoPendienteAgente> pendientes = await cola.ObtenerPendientesAsync(
            10,
            DateTimeOffset.UtcNow.AddSeconds(1),
            CancellationToken.None
        );
        Assert.Single(pendientes);
        Assert.Equal("PENDING", pendientes[0].Estado);
    }

    [Fact]
    public async Task MarcarFallidoMueveADeadLetterAlSuperarIntentos()
    {
        ColaEventosAgenteSqlite cola = CrearCola();
        await cola.InicializarAsync(CancellationToken.None);
        EventoPendienteAgente evento = await cola.EncolarAsync("ROLLBACK_FAILED", "{\"ok\":false}", "evt-3", CancellationToken.None);

        await cola.MarcarFallidoAsync(
            evento.Id,
            "Backend no disponible",
            maxIntentos: 1,
            DateTimeOffset.UtcNow.AddSeconds(-1),
            CancellationToken.None
        );

        IReadOnlyList<EventoPendienteAgente> pendientes = await cola.ObtenerPendientesAsync(
            10,
            DateTimeOffset.UtcNow.AddSeconds(1),
            CancellationToken.None
        );
        Assert.Empty(pendientes);

        DiagnosticoColaEventosAgente diagnostico = await cola.ObtenerDiagnosticoAsync(CancellationToken.None);
        Assert.Equal(1, diagnostico.DeadLetter);
        Assert.Equal("Backend no disponible", diagnostico.UltimoErrorDeadLetter);
    }

    [Fact]
    public async Task DespachadorEnviaEventoEncoladoABackendHttp()
    {
        ColaEventosAgenteSqlite cola = CrearCola();
        await cola.InicializarAsync(CancellationToken.None);

        Guid idEquipo = Guid.NewGuid();
        using var handler = new CapturadorHttpHandler(HttpStatusCode.Accepted);
        var clienteRemoto = new ClienteOperacionesFarmamia(new HttpClient(handler)
        {
            BaseAddress = new Uri("http://farmamia.test")
        });
        var clienteConCola = new ClienteOperacionesFarmamiaConCola(
            clienteRemoto,
            cola,
            NullLogger<ClienteOperacionesFarmamiaConCola>.Instance
        );
        var despachador = new DespachadorColaEventosServicio(
            cola,
            new ConfiguracionLocalFake(new CredencialesAgente(idEquipo, "token-local")),
            clienteRemoto,
            NullLogger<DespachadorColaEventosServicio>.Instance,
            Options.Create(new OpcionesAgente())
        );

        await clienteConCola.ReportarEventoAsync(
            new CredencialesAgente(idEquipo, "token-local"),
            new EventoAgente(
                null,
                "evt-http-1",
                "UPDATE_STARTED",
                "inicio",
                "1.0",
                "1.1",
                new Dictionary<string, object?>()
            ),
            CancellationToken.None
        );

        await despachador.DespacharPendientesUnaVezAsync(CancellationToken.None);

        DiagnosticoColaEventosAgente diagnostico = await cola.ObtenerDiagnosticoAsync(CancellationToken.None);
        Assert.Equal(1, diagnostico.Enviados);
        Assert.Equal(0, diagnostico.Pendientes);
        Assert.Single(handler.Solicitudes);
        Assert.Equal($"/api/agent/{idEquipo}/events", handler.Solicitudes[0].RequestUri?.PathAndQuery);
        Assert.Equal("Bearer", handler.Solicitudes[0].Headers.Authorization?.Scheme);
    }

    private ColaEventosAgenteSqlite CrearCola()
    {
        return new ColaEventosAgenteSqlite(Options.Create(new OpcionesAgente
        {
            RutaAgente = rutaTemporal
        }));
    }

    public void Dispose()
    {
        if (Directory.Exists(rutaTemporal))
        {
            Directory.Delete(rutaTemporal, recursive: true);
        }
    }

    private sealed class ConfiguracionLocalFake : IConfiguracionLocalAgente
    {
        private readonly CredencialesAgente credenciales;

        public ConfiguracionLocalFake(CredencialesAgente credenciales)
        {
            this.credenciales = credenciales;
        }

        public Task PrepararEstructuraAsync(CancellationToken cancellationToken)
        {
            return Task.CompletedTask;
        }

        public Task<CredencialesAgente?> LeerCredencialesAsync(CancellationToken cancellationToken)
        {
            return Task.FromResult<CredencialesAgente?>(credenciales);
        }

        public Task GuardarCredencialesAsync(CredencialesAgente credenciales, CancellationToken cancellationToken)
        {
            return Task.CompletedTask;
        }
    }

    private sealed class CapturadorHttpHandler : HttpMessageHandler, IDisposable
    {
        private readonly HttpStatusCode statusCode;

        public CapturadorHttpHandler(HttpStatusCode statusCode)
        {
            this.statusCode = statusCode;
        }

        public List<HttpRequestMessage> Solicitudes { get; } = [];

        protected override async Task<HttpResponseMessage> SendAsync(
            HttpRequestMessage request,
            CancellationToken cancellationToken
        )
        {
            var copia = new HttpRequestMessage(request.Method, request.RequestUri);
            copia.Headers.Authorization = request.Headers.Authorization;
            if (request.Content is not null)
            {
                copia.Content = new StringContent(await request.Content.ReadAsStringAsync(cancellationToken));
            }

            Solicitudes.Add(copia);
            return new HttpResponseMessage(statusCode);
        }

        public new void Dispose()
        {
            foreach (HttpRequestMessage solicitud in Solicitudes)
            {
                solicitud.Dispose();
            }

            base.Dispose();
        }
    }
}
