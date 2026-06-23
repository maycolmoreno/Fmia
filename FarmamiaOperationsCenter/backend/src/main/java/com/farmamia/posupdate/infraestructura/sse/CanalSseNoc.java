package com.farmamia.posupdate.infraestructura.sse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Canal SSE exclusivo para el panel Angular NOC.
 * Los agentes usan CanalSseAgentes (instrucciones push).
 * Este canal difunde eventos operativos al frontend en tiempo real.
 */
@Component
public class CanalSseNoc {

    private static final Logger LOG = LoggerFactory.getLogger(CanalSseNoc.class);
    private static final long TIMEOUT_MS = 300_000L; // 5 minutos

    private final ConcurrentHashMap<String, SseEmitter> sesiones = new ConcurrentHashMap<>();

    public SseEmitter conectar(String idSesion) {
        SseEmitter emisor = new SseEmitter(TIMEOUT_MS);
        sesiones.put(idSesion, emisor);
        emisor.onCompletion(() -> sesiones.remove(idSesion));
        emisor.onTimeout(() -> sesiones.remove(idSesion));
        emisor.onError(ex -> sesiones.remove(idSesion));
        LOG.debug("Panel NOC conectado: sesion={}. Sesiones activas: {}", idSesion, sesiones.size());
        return emisor;
    }

    public void emitirProgresoDescarga(UUID idObjetivoDespliegue, UUID idEquipo, BigDecimal porcentaje) {
        if (sesiones.isEmpty()) return;
        String json = String.format(
            "{\"tipo\":\"PROGRESO_DESCARGA\",\"idObjetivoDespliegue\":\"%s\",\"idEquipo\":\"%s\",\"progreso\":%.2f}",
            idObjetivoDespliegue, idEquipo, porcentaje
        );
        difundir("progreso_descarga", json);
    }

    @Scheduled(fixedRate = 25_000)
    public void enviarLatido() {
        if (sesiones.isEmpty()) return;
        difundir("ping", "");
    }

    private void difundir(String nombreEvento, String datos) {
        sesiones.forEach((idSesion, emisor) -> {
            try {
                emisor.send(SseEmitter.event().name(nombreEvento).data(datos));
            } catch (IOException e) {
                sesiones.remove(idSesion);
                LOG.debug("Sesion NOC {} removida (canal cerrado)", idSesion);
            }
        });
    }

    public int sesionesActivas() {
        return sesiones.size();
    }
}
