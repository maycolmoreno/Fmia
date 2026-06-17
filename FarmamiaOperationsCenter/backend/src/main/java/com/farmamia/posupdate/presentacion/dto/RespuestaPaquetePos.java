package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RespuestaPaquetePos(
    @JsonProperty("id") UUID id,
    @JsonProperty("version") String version,
    @JsonProperty("fileName") String nombreArchivo,
    @JsonProperty("sha256Checksum") String checksumSha256,
    @JsonProperty("signature") String firma,
    @JsonProperty("signatureAlgorithm") String algoritmoFirma,
    @JsonProperty("signingKeyId") String idClaveFirma,
    @JsonProperty("signatureStatus") String estadoFirma,
    @JsonProperty("sizeBytes") Long tamanoBytes,
    @JsonProperty("status") String estado,
    @JsonProperty("downloadUrl") String urlDescarga,
    @JsonProperty("uploadedAt") OffsetDateTime cargadoEn,
    @JsonProperty("approvedAt") OffsetDateTime aprobadoEn
) {
}
