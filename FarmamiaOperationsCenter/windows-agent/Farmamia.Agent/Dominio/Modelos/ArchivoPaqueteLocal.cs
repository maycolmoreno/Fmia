namespace Farmamia.Agent.Dominio.Modelos;

public sealed record ArchivoPaqueteLocal(
    string RutaArchivo,
    long TamanoBytes,
    string ChecksumSha256
);
