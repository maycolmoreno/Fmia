namespace Farmamia.Agent.Dominio.Modelos;

public sealed record EstadoLocalAgente(
    string UltimaAccion,
    string? UltimaVersion,
    string? UltimoResultado,
    string? UltimoErrorTecnico,
    Guid? IdObjetivoDespliegue,
    Guid? IdPaquete,
    DateTimeOffset ActualizadoEn
);
