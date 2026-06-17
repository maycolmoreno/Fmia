package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RespuestaPagina<T>(
    @JsonProperty("content") List<T> contenido,
    @JsonProperty("page") int pagina,
    @JsonProperty("size") int tamano,
    @JsonProperty("totalElements") long totalElementos,
    @JsonProperty("totalPages") int totalPaginas,
    @JsonProperty("hasNext") boolean tieneSiguiente
) {
}
