package com.farmamia.posupdate.presentacion.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record SolicitudProgresoDescarga(
    @NotNull UUID idObjetivoDespliegue,
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal progreso
) {}
