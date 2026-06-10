package com.farmamia.operations.dominio.puerto;

public interface CodificadorContrasenas {

    boolean coincide(String contrasenaPlano, String hashContrasena);

    String codificar(String contrasenaPlano);
}
