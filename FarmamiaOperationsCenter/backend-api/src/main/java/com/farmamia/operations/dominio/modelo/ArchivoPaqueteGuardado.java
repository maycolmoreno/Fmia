package com.farmamia.operations.dominio.modelo;

public record ArchivoPaqueteGuardado(
    String nombreArchivo,
    String rutaAlmacenamiento,
    String checksumSha256,
    long tamanoBytes
) {
}
