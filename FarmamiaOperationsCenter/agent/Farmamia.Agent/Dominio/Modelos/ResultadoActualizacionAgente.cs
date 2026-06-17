namespace Farmamia.Agent.Dominio.Modelos;

public sealed record ResultadoActualizacionAgente(
    Guid IdObjetivoDespliegue,
    string IdempotencyKey,
    string Estado,
    string? VersionAnterior,
    string? VersionNueva,
    string? Mensaje
);
