namespace Farmamia.Agent.Dominio.Modelos;

public sealed record LatidoAgente(
    Guid IdEquipo,
    string VersionPos,
    long DiscoLibreMb,
    long DiscoTotalMb,
    bool ProcesoPosEjecutandose,
    int LatenciaMs,
    decimal PorcentajePerdidaPaquetes
);
