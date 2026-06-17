package com.farmamia.posupdate.presentacion.controlador;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

final class PermisosAdministrativos {

    private PermisosAdministrativos() {
    }

    static void exigirRol(Authentication autenticacion, String mensaje, String... roles) {
        Set<String> rolesPermitidos = Arrays.stream(roles)
            .map(rol -> "ROLE_" + rol)
            .collect(Collectors.toSet());

        boolean permitido = autenticacion != null && autenticacion.getAuthorities().stream()
            .anyMatch(autoridad -> rolesPermitidos.contains(autoridad.getAuthority()));

        if (!permitido) {
            throw new AccessDeniedException(mensaje);
        }
    }
}
