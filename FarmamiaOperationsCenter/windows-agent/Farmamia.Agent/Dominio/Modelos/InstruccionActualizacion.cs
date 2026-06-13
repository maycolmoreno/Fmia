namespace Farmamia.Agent.Dominio.Modelos;

public sealed record InstruccionActualizacion(
    bool TieneInstruccion,
    string? TipoInstruccion,
    Guid? IdObjetivoDespliegue,
    Guid? IdPaquete,
    string? Version,
    string? UrlDescarga,
    string? ChecksumSha256,
    string? Firma,
    string? AlgoritmoFirma,
    string? IdClaveFirma,
    string? ClavePublicaFirmaPem,
    TimeOnly? HoraOficialActualizacion,
    TimeOnly? HoraForzadaActualizacion,
    IReadOnlyList<TimeOnly> Avisos
);
