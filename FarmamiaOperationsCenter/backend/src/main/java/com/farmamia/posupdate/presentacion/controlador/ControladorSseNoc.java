package com.farmamia.posupdate.presentacion.controlador;

import com.farmamia.posupdate.infraestructura.sse.CanalSseNoc;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Punto de suscripción SSE para el panel Angular NOC.
 * Angular se conecta aquí para recibir eventos en tiempo real
 * (progreso de descarga, alertas, etc.) sin polling.
 *
 * GET /api/noc/stream?sesion={uuid-aleatorio-del-cliente}
 */
@RestController
@RequestMapping("/api/noc")
public class ControladorSseNoc {

    private final CanalSseNoc canalSseNoc;

    public ControladorSseNoc(CanalSseNoc canalSseNoc) {
        this.canalSseNoc = canalSseNoc;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribir(@RequestParam(defaultValue = "") String sesion) {
        String idSesion = sesion.isBlank() ? UUID.randomUUID().toString() : sesion;
        return canalSseNoc.conectar(idSesion);
    }
}
