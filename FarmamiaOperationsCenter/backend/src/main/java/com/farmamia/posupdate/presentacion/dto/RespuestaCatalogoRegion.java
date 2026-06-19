package com.farmamia.posupdate.presentacion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RespuestaCatalogoRegion(
    @JsonProperty("region") String region,
    @JsonProperty("provincias") List<String> provincias
) {
}
