package com.farmamia.posupdate.infraestructura.seguridad;

import com.farmamia.posupdate.dominio.puerto.CodificadorContrasenas;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdaptadorCodificadorContrasenas implements CodificadorContrasenas {

    private final PasswordEncoder passwordEncoder;

    public AdaptadorCodificadorContrasenas(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean coincide(String contrasenaPlano, String hashContrasena) {
        return passwordEncoder.matches(contrasenaPlano, hashContrasena);
    }

    @Override
    public String codificar(String contrasenaPlano) {
        return passwordEncoder.encode(contrasenaPlano);
    }
}
