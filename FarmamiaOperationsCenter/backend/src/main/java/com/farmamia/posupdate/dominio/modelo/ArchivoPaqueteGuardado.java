package com.farmamia.posupdate.dominio.modelo;

public record ArchivoPaqueteGuardado(
    String nombreArchivo,
    String rutaAlmacenamiento,
    String checksumSha256,
    long tamanoBytes
) {
}
