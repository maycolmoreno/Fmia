package com.farmamia.posupdate.presentacion.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.List;

// Reproduce el bug real encontrado al probar la Fase 3 en la app corriendo: Spring envuelve
// cualquier IllegalArgumentException que escape de un bean @Repository en
// InvalidDataAccessApiUsageException (asumiendo mal uso de JPA), aunque sea una validacion de
// negocio nuestra. Sin manejarUsoInvalidoAccesoDatos, esto llegaba al cliente como 500 generico
// en vez del 400 con el mensaje real.
class ManejadorExcepcionesApiTest {

    private final ManejadorExcepcionesApi manejador = new ManejadorExcepcionesApi();

    @Test
    void desenvuelveIllegalArgumentExceptionComoBadRequest() {
        IllegalArgumentException causaOriginal = new IllegalArgumentException("Ya existe una actualizacion POS activa para 1 farmacia(s) objetivo");
        InvalidDataAccessApiUsageException envuelta = new InvalidDataAccessApiUsageException("wrapper", causaOriginal);

        var respuesta = manejador.manejarUsoInvalidoAccesoDatos(envuelta);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("INVALID_REQUEST", respuesta.getBody().code());
        assertEquals(causaOriginal.getMessage(), respuesta.getBody().message());
    }

    @Test
    void mantieneComo500SiLaCausaNoEsDeValidacionDeNegocio() {
        RuntimeException causaGenuinaDeJpa = new RuntimeException("fallo real de acceso a datos");
        InvalidDataAccessApiUsageException envuelta = new InvalidDataAccessApiUsageException("wrapper", causaGenuinaDeJpa);

        var respuesta = manejador.manejarUsoInvalidoAccesoDatos(envuelta);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        assertEquals("INTERNAL_ERROR", respuesta.getBody().code());
    }

    @Test
    void manejaIllegalArgumentExceptionDirectaComoBadRequest() {
        var respuesta = manejador.manejarArgumentoInvalido(new IllegalArgumentException("mensaje de prueba"));

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("mensaje de prueba", respuesta.getBody().message());
    }

    // Reproduce el bug real de Fase 0: dos equipos con el mismo mac_address (FortiClient asigna
    // el mismo MAC fijo en todas las maquinas) violaban uq_devices_mac_address y el cliente
    // recibia 500 generico en vez de un 409 identificable.
    @Test
    void manejaViolacionDeConstraintComoConflicto() {
        ConstraintViolationException causaConstraint = new ConstraintViolationException(
            "could not execute statement",
            new java.sql.SQLException("duplicate key"),
            "uq_devices_mac_address"
        );
        DataIntegrityViolationException envuelta = new DataIntegrityViolationException("wrapper", causaConstraint);

        var respuesta = manejador.manejarViolacionIntegridadDatos(envuelta);

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("DATA_CONFLICT", respuesta.getBody().code());
        assertEquals("El cambio viola una restriccion existente: uq_devices_mac_address", respuesta.getBody().message());
    }

    @Test
    void manejaViolacionDeIntegridadSinNombreDeConstraintConMensajeGenerico() {
        DataIntegrityViolationException envuelta = new DataIntegrityViolationException(
            "wrapper",
            new RuntimeException("fallo de integridad sin detalle de constraint")
        );

        var respuesta = manejador.manejarViolacionIntegridadDatos(envuelta);

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("DATA_CONFLICT", respuesta.getBody().code());
        assertEquals("El cambio no se pudo aplicar por un conflicto de datos.", respuesta.getBody().message());
    }

    // Reproduce el otro bug real de la misma sesion: un cliente que envia Content-Type incorrecto
    // (ej. application/x-www-form-urlencoded en vez de application/json) a un endpoint con
    // @RequestBody caia en el catch-all de Exception y devolvia 500 en vez de 415.
    @Test
    void manejaContentTypeNoSoportadoComo415() {
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(
            MediaType.APPLICATION_FORM_URLENCODED,
            List.of(MediaType.APPLICATION_JSON)
        );

        var respuesta = manejador.manejarTipoContenidoNoSoportado(ex);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, respuesta.getStatusCode());
        assertEquals("UNSUPPORTED_MEDIA_TYPE", respuesta.getBody().code());
    }

    // Reproduce el bug encontrado al verificar en vivo el colapso de superficies de pausa
    // redundantes (POST /grupos-trx/{id}/pausar|reanudar eliminados): Spring señala la ruta
    // inexistente con NoResourceFoundException, pero el catch-all de Exception la convertia en
    // 500 en vez del 404 real.
    @Test
    void manejaRutaInexistenteComo404() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.POST, "api/deployments/x/grupos-trx/y/pausar");

        var respuesta = manejador.manejarRecursoDeFrameworkNoEncontrado(ex);

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("RESOURCE_NOT_FOUND", respuesta.getBody().code());
    }
}
