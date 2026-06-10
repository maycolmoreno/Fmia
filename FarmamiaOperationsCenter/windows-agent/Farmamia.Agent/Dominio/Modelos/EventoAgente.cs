namespace Farmamia.Agent.Dominio.Modelos;

public sealed record EventoAgente(
    Guid? IdObjetivoDespliegue,
    string TipoEvento,
    string? MensajeEvento,
    string? VersionAnterior,
    string? VersionNueva,
    IReadOnlyDictionary<string, object?> Metadatos
);
