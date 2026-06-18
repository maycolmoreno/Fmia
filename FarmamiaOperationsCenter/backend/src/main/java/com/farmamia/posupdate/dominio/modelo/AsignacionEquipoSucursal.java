package com.farmamia.posupdate.dominio.modelo;

import java.util.UUID;

public record AsignacionEquipoSucursal(
    UUID idEquipo,
    UUID idSucursal
) {
}
