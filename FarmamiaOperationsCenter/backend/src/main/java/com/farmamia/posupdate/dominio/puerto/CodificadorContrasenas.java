package com.farmamia.posupdate.dominio.puerto;

public interface CodificadorContrasenas {

    boolean coincide(String contrasenaPlano, String hashContrasena);

    String codificar(String contrasenaPlano);
}
