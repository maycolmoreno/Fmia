package com.farmamia.posupdate.dominio.modelo;

import java.util.UUID;

public record SucursalSugerida(
    UUID id,
    String codigo,
    String nombre,
    String codigoGrupoTrx
) {
}
