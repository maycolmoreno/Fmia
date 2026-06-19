package com.farmamia.posupdate.dominio.modelo;

import java.util.List;

public record CatalogoRegion(
    String region,
    List<String> provincias
) {
}
