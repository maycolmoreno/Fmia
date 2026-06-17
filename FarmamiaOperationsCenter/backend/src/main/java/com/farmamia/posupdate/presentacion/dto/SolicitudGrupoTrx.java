package com.farmamia.posupdate.presentacion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SolicitudGrupoTrx(
    @NotBlank(message = "El codigo es obligatorio")
    @Pattern(regexp = "^trx[0-9]{3}$", message = "El codigo debe tener formato trx001")
    String codigo,
    @NotBlank(message = "El nombre es obligatorio")
    String nombre,
    String descripcion,
    @Min(value = 1, message = "El maximo debe ser al menos 1")
    @Max(value = 100, message = "El maximo no puede superar 100 equipos")
    Integer maximoEquipos,
    Boolean activo
) {
}
