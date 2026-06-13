namespace Farmamia.Agent.Dominio.Modelos;

public sealed record DiagnosticoColaEventosAgente(
    int Pendientes,
    int Enviando,
    int Enviados,
    int Fallidos,
    int DeadLetter,
    DateTimeOffset? EventoMasAntiguoPendiente,
    DateTimeOffset? UltimoDeadLetterEn,
    string? UltimoErrorDeadLetter
);
