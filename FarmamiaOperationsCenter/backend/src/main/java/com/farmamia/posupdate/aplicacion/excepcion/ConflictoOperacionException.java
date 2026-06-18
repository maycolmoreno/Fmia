package com.farmamia.posupdate.aplicacion.excepcion;

public class ConflictoOperacionException extends RuntimeException {

    public ConflictoOperacionException(String mensaje) {
        super(mensaje);
    }
}
