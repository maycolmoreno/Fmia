package com.farmamia.operations.presentacion.controlador;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.aplicacion.excepcion.ConflictoIdempotenciaException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorExcepcionesApi {

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

    @ExceptionHandler(ConflictoIdempotenciaException.class)
    public ResponseEntity<RespuestaErrorApi> manejarConflictoIdempotencia(ConflictoIdempotenciaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(RespuestaErrorApi.de("IDEMPOTENCY_CONFLICT", ex.getMessage()));
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
