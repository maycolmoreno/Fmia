namespace Farmamia.Agent.Dominio.Modelos;

public sealed record DatosInventarioEquipo(
    string NombreEquipo,
    string DireccionIp,
    string DireccionMac,
    string VersionWindows,
    string VersionAgente,
    string VersionPos,
    string RutaPos,
    long DiscoLibreMb,
    long DiscoTotalMb,
    bool ProcesoPosEjecutandose
);
