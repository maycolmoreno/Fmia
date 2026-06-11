using Farmamia.Agent.Dominio.Modelos;
using Farmamia.Agent.Infraestructura.Cola;
using Microsoft.Extensions.Options;
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
}
