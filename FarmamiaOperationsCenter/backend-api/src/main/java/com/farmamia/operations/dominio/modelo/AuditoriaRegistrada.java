package com.farmamia.operations.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditoriaRegistrada(
    UUID id,
    UUID idUsuarioActor,
    String usuarioActor,
    String accion,
    String tipoEntidad,
    UUID idEntidad,
    Map<String, Object> valoresAnteriores,
    Map<String, Object> valoresNuevos,
    String direccionIp,
    OffsetDateTime creadoEn
) {
}
