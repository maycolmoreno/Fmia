package com.farmamia.operations.aplicacion.excepcion;

public class ConflictoIdempotenciaException extends RuntimeException {

    public ConflictoIdempotenciaException(String mensaje) {
        super(mensaje);
    }
}
