package com.farmamia.operations.dominio.modelo;

public record FiltroUsuariosAdministrativos(
    String q,
    String rol,
    Boolean activo,
    Boolean bloqueado,
    int pagina,
    int tamano,
    String orden
) {
}
