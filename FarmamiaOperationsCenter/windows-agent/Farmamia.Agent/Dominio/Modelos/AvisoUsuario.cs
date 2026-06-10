namespace Farmamia.Agent.Dominio.Modelos;

public sealed record AvisoUsuario(
    Guid IdObjetivoDespliegue,
    string VersionNueva,
    TimeOnly HoraAviso,
    TimeOnly? HoraForzadaActualizacion,
    string Mensaje
);
