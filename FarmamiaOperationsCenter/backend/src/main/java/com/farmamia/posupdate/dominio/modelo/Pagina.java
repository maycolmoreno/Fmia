package com.farmamia.posupdate.dominio.modelo;

import java.util.List;

public record Pagina<T>(
    List<T> contenido,
    int pagina,
    int tamano,
    long totalElementos,
    int totalPaginas,
    boolean tieneSiguiente
) {
}
