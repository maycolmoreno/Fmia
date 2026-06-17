package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record RespuestaAuditoria(
    @JsonProperty("id") UUID id,
    @JsonProperty("actorUserId") UUID idUsuarioActor,
    @JsonProperty("actorUsername") String usuarioActor,
    @JsonProperty("action") String accion,
    @JsonProperty("entityType") String tipoEntidad,
    @JsonProperty("entityId") UUID idEntidad,
    @JsonProperty("oldValues") Map<String, Object> valoresAnteriores,
    @JsonProperty("newValues") Map<String, Object> valoresNuevos,
    @JsonProperty("ipAddress") String direccionIp,
    @JsonProperty("createdAt") OffsetDateTime creadoEn
) {
}
