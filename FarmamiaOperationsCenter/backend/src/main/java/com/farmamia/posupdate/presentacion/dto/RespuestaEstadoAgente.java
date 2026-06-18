package com.farmamia.posupdate.presentacion.dto;

import java.util.UUID;

public record RespuestaEstadoAgente(
    UUID idEquipo,
    boolean online
) {}
