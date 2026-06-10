namespace Farmamia.Agent.Dominio.Modelos;

public sealed record InstruccionActualizacion(
    bool TieneInstruccion,
    string? TipoInstruccion,
    Guid? IdObjetivoDespliegue,
    Guid? IdPaquete,
    string? Version,
    string? UrlDescarga,
    string? ChecksumSha256,
    TimeOnly? HoraOficialActualizacion,
    TimeOnly? HoraForzadaActualizacion,
    IReadOnlyList<TimeOnly> Avisos
);
