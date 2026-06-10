package com.farmamia.operations.dominio.modelo;

import java.util.UUID;

public record UsuarioAdministrativo(
    UUID id,
    String usuario,
    String hashContrasena,
    String nombreCompleto,
    String correo,
    String rol,
    boolean activo,
    int intentosFallidosLogin,
    java.time.OffsetDateTime bloqueadoHasta,
    java.time.OffsetDateTime ultimoAccesoEn,
    java.time.OffsetDateTime creadoEn,
    java.time.OffsetDateTime actualizadoEn
) {
}
