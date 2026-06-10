package com.farmamia.operations.dominio.modelo;

import java.io.InputStream;

public record ArchivoPaqueteDescarga(
    String nombreArchivo,
    InputStream contenido
) {
}
