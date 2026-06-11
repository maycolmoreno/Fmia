namespace Farmamia.Agent.Dominio.Modelos;

public sealed record EventoPendienteAgente(
    Guid Id,
    string TipoEvento,
    string PayloadJson,
    string IdempotencyKey,
    string Estado,
    int CantidadIntentos,
    DateTimeOffset ProximoIntentoEn,
    DateTimeOffset CreadoEn,
    DateTimeOffset ActualizadoEn,
    string? UltimoError,
    string Checksum
);
