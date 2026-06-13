using Farmamia.Agent.Dominio.Modelos;

namespace Farmamia.Agent.Dominio.Puertos;

public interface IColaEventosAgente
{
    Task InicializarAsync(CancellationToken cancellationToken);

    Task RecuperarEnviosInterrumpidosAsync(CancellationToken cancellationToken);

    Task<EventoPendienteAgente> EncolarAsync(
        string tipoEvento,
        string payloadJson,
        string? idempotencyKey,
        CancellationToken cancellationToken
    );

    Task<IReadOnlyList<EventoPendienteAgente>> ObtenerPendientesAsync(
        int limite,
        DateTimeOffset ahora,
        CancellationToken cancellationToken
    );

    Task<DiagnosticoColaEventosAgente> ObtenerDiagnosticoAsync(CancellationToken cancellationToken);

    Task MarcarEnviandoAsync(Guid id, CancellationToken cancellationToken);

    Task MarcarEnviadoAsync(Guid id, CancellationToken cancellationToken);

    Task MarcarFallidoAsync(
        Guid id,
        string error,
        int maxIntentos,
        DateTimeOffset proximoIntentoEn,
        CancellationToken cancellationToken
    );
}
