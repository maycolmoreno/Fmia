package com.farmamia.operations.dominio.modelo;

import java.util.Map;
import java.util.UUID;

public record DatosAuditoria(
    String usuarioActor,
    String accion,
    String tipoEntidad,
    UUID idEntidad,
    Map<String, Object> valoresAnteriores,
    Map<String, Object> valoresNuevos,
    String direccionIp
) {
}
