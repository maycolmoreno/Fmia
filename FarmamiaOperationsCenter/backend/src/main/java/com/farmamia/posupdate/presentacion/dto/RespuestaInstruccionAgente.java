package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaInstruccionAgente(
    @JsonProperty("hasInstruction") boolean tieneInstruccion,
    @JsonProperty("instructionType") String tipoInstruccion,
    @JsonProperty("deploymentTargetId") UUID idObjetivoDespliegue,
    @JsonProperty("packageId") UUID idPaquete,
    @JsonProperty("version") String version,
    @JsonProperty("downloadUrl") String urlDescarga,
    @JsonProperty("sha256Checksum") String checksumSha256,
    @JsonProperty("signature") String firma,
    @JsonProperty("signatureAlgorithm") String algoritmoFirma,
    @JsonProperty("signingKeyId") String idClaveFirma,
    @JsonProperty("signingPublicKeyPem") String clavePublicaFirmaPem,
    @JsonProperty("officialUpdateTime") LocalTime horaOficialActualizacion,
    @JsonProperty("forceUpdateTime") LocalTime horaForzadaActualizacion,
    @JsonProperty("warnings") List<LocalTime> avisos
) {
    public static RespuestaInstruccionAgente vacia() {
        return new RespuestaInstruccionAgente(false, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
