package com.farmamia.posupdate.presentacion.controlador;

import com.farmamia.posupdate.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.posupdate.aplicacion.excepcion.ConflictoIdempotenciaException;
import com.farmamia.posupdate.aplicacion.excepcion.ConflictoOperacionException;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorExcepcionesApi {

    private static final Logger log = LoggerFactory.getLogger(ManejadorExcepcionesApi.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaErrorApi> manejarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(RespuestaErrorApi.de("RESOURCE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaErrorApi> manejarValidacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::formatearErrorCampo)
            .toList();

        return ResponseEntity.badRequest()
            .body(RespuestaErrorApi.de("VALIDATION_ERROR", "Request validation failed", detalles));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RespuestaErrorApi> manejarArgumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
            .body(RespuestaErrorApi.de("INVALID_REQUEST", ex.getMessage()));
    }

    // Spring envuelve cualquier IllegalArgumentException/IllegalStateException que escape de un
    // bean @Repository (como los adaptadores JPA) en InvalidDataAccessApiUsageException, asumiendo
    // que es un mal uso de JPA — aunque en realidad sea una validacion de negocio nuestra (ej.
    // RepositorioDesplieguesJpaAdaptador.crear() valida reglas de campana en la misma clase que
    // hace persistencia). Sin este handler, esas validaciones terminaban como 500 en vez de 400.
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<RespuestaErrorApi> manejarUsoInvalidoAccesoDatos(InvalidDataAccessApiUsageException ex) {
        Throwable causaMasEspecifica = ex.getMostSpecificCause();
        if (causaMasEspecifica instanceof IllegalArgumentException causaArgumentoInvalido) {
            return manejarArgumentoInvalido(causaArgumentoInvalido);
        }
        log.error("InvalidDataAccessApiUsageException no reconocida", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(RespuestaErrorApi.de("INTERNAL_ERROR", "Unexpected server error"));
    }

    // Encontrado al probar Fase 0/Fase 1 end-to-end: dos equipos distintos reportando el mismo
    // mac_address (FortiClient asigna el mismo MAC fijo en todas las maquinas) violaban
    // uq_devices_mac_address y llegaban al cliente como 500 generico en vez de un conflicto
    // identificable. DataIntegrityViolationException es la traduccion estandar de Spring para
    // cualquier violacion de constraint (unique, foreign key, check) — se mapea a 409 con el
    // nombre de la constraint cuando esta disponible, sin exponer el SQL crudo al cliente.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespuestaErrorApi> manejarViolacionIntegridadDatos(DataIntegrityViolationException ex) {
        String mensaje = "El cambio no se pudo aplicar por un conflicto de datos.";
        ConstraintViolationException causaConstraint = buscarCausaConstraint(ex);
        if (causaConstraint != null && causaConstraint.getConstraintName() != null) {
            mensaje = "El cambio viola una restriccion existente: " + causaConstraint.getConstraintName();
        }

        log.warn("Violacion de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(RespuestaErrorApi.de("DATA_CONFLICT", mensaje));
    }

    // getMostSpecificCause() llega hasta el SQLException interno, un nivel mas profundo que
    // ConstraintViolationException (que es donde Hibernate expone el nombre de la constraint) —
    // hay que buscarla explicitamente en la cadena de causas en vez de usar la mas profunda.
    private ConstraintViolationException buscarCausaConstraint(Throwable ex) {
        Throwable actual = ex.getCause();
        while (actual != null && !(actual instanceof ConstraintViolationException)) {
            actual = actual.getCause();
        }
        return actual instanceof ConstraintViolationException causaConstraint ? causaConstraint : null;
    }

    // Encontrado en la misma sesion de pruebas: un cliente que envia Content-Type incorrecto
    // (ej. application/x-www-form-urlencoded en vez de application/json) a un endpoint con
    // @RequestBody caia en el catch-all de Exception y devolvia 500 en vez de 415.
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<RespuestaErrorApi> manejarTipoContenidoNoSoportado(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(RespuestaErrorApi.de("UNSUPPORTED_MEDIA_TYPE", ex.getMessage()));
    }

    @ExceptionHandler(ConflictoIdempotenciaException.class)
    public ResponseEntity<RespuestaErrorApi> manejarConflictoIdempotencia(ConflictoIdempotenciaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(RespuestaErrorApi.de("IDEMPOTENCY_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(ConflictoOperacionException.class)
    public ResponseEntity<RespuestaErrorApi> manejarConflictoOperacion(ConflictoOperacionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(RespuestaErrorApi.de("OPERATION_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<RespuestaErrorApi> manejarCredencialesInvalidas(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(RespuestaErrorApi.de("AUTHENTICATION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespuestaErrorApi> manejarAccesoDenegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(RespuestaErrorApi.de("ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaErrorApi> manejarInesperado(Exception ex) {
        log.error("Error inesperado no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(RespuestaErrorApi.de("INTERNAL_ERROR", "Unexpected server error"));
    }

    private String formatearErrorCampo(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    public record RespuestaErrorApi(
        String code,
        String message,
        List<String> details,
        OffsetDateTime timestamp
    ) {
        static RespuestaErrorApi de(String codigo, String mensaje) {
            return de(codigo, mensaje, List.of());
        }

        static RespuestaErrorApi de(String codigo, String mensaje, List<String> detalles) {
            return new RespuestaErrorApi(codigo, mensaje, detalles, OffsetDateTime.now());
        }
    }
}
