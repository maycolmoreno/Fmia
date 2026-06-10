package com.farmamia.operations.dominio.modelo;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record InstruccionAgente(
    boolean tieneInstruccion,
    String tipoInstruccion,
    UUID idObjetivoDespliegue,
    UUID idPaquete,
    String version,
    String urlDescarga,
    String checksumSha256,
    LocalTime horaOficialActualizacion,
    LocalTime horaForzadaActualizacion,
    List<LocalTime> avisos
) {
    public static InstruccionAgente vacia() {
        return new InstruccionAgente(false, null, null, null, null, null, null, null, null, null);
    }
}
