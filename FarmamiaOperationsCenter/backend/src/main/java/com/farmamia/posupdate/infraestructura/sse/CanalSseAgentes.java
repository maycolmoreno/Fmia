package com.farmamia.posupdate.infraestructura.sse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class CanalSseAgentes {

    private static final Logger LOG = LoggerFactory.getLogger(CanalSseAgentes.class);

    private final ConcurrentHashMap<UUID, SseEmitter> emisores = new ConcurrentHashMap<>();

    public SseEmitter conectar(UUID idEquipo) {
        SseEmitter emisor = new SseEmitter(Long.MAX_VALUE);
        emisores.put(idEquipo, emisor);
        emisor.onCompletion(() -> emisores.remove(idEquipo, emisor));
        emisor.onTimeout(() -> emisores.remove(idEquipo, emisor));
        emisor.onError(e -> emisores.remove(idEquipo, emisor));
        LOG.debug("Agente {} conectado al canal SSE. Conexiones activas: {}", idEquipo, emisores.size());
        return emisor;
    }

    public void notificarInstruccionDisponible(UUID idEquipo) {
        SseEmitter emisor = emisores.get(idEquipo);
        if (emisor == null) return;
        try {
            emisor.send(SseEmitter.event().name("instruccion_disponible").data(""));
        } catch (IOException e) {
            emisores.remove(idEquipo, emisor);
            LOG.debug("Agente {} desconectado al notificar (canal cerrado)", idEquipo);
        }
    }

    public void notificarTodosConectados() {
        if (emisores.isEmpty()) return;
        LOG.debug("Notificando instruccion_disponible a {} agentes conectados", emisores.size());
        emisores.forEach((idEquipo, emisor) -> {
            try {
                emisor.send(SseEmitter.event().name("instruccion_disponible").data(""));
            } catch (IOException e) {
                emisores.remove(idEquipo, emisor);
            }
        });
    }

    public int conexionesActivas() {
        return emisores.size();
    }
}
