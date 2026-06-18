package com.farmamia.posupdate.infraestructura.sse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class CanalSseAgentes {

    private static final Logger LOG = LoggerFactory.getLogger(CanalSseAgentes.class);
    private static final long TIMEOUT_MS = 300000L; // 5 minutos

    private final ConcurrentHashMap<UUID, SseEmitter> emisores = new ConcurrentHashMap<>();
    private final Counter pingsFallidos;

    public CanalSseAgentes(MeterRegistry meterRegistry) {
        this.pingsFallidos = Counter.builder("farmamia.sse.pings.failed.total")
            .description("Total acumulado de pings fallidos debido a conexiones zombis removidas")
            .register(meterRegistry);

        Gauge.builder("farmamia.sse.connections.active", this.emisores, Map::size)
            .description("Numero de conexiones activas en el canal SSE de agentes")
            .strongReference(true)
            .register(meterRegistry);
    }

    public SseEmitter conectar(UUID idEquipo) {
        SseEmitter emisor = new SseEmitter(TIMEOUT_MS);
        emisores.put(idEquipo, emisor);

        emisor.onCompletion(() -> removerAgente(idEquipo));
        emisor.onTimeout(() -> removerAgente(idEquipo));
        emisor.onError((ex) -> removerAgente(idEquipo));

        LOG.debug("Agente {} conectado al canal SSE. Conexiones activas: {}", idEquipo, emisores.size());
        return emisor;
    }

    @Scheduled(fixedRate = 25000)
    public void enviarLatido() {
        if (emisores.isEmpty()) return;
        LOG.trace("Despachando latido ping a {} agentes conectados", emisores.size());
        emisores.forEach((idEquipo, emisor) -> {
            try {
                emisor.send(SseEmitter.event().name("ping").data(""));
            } catch (IOException e) {
                this.pingsFallidos.increment();
                removerAgente(idEquipo);
                LOG.debug("Agente {} detectado zombi durante latido y removido del canal", idEquipo);
            }
        });
    }

    public void notificarInstruccionDisponible(UUID idEquipo) {
        SseEmitter emisor = emisores.get(idEquipo);
        if (emisor == null) return;
        try {
            emisor.send(SseEmitter.event().name("instruccion_disponible").data(""));
        } catch (IOException e) {
            removerAgente(idEquipo);
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
                removerAgente(idEquipo);
            }
        });
    }

    public int conexionesActivas() {
        return emisores.size();
    }

    public boolean estaAgenteConectado(UUID idEquipo) {
        return emisores.containsKey(idEquipo);
    }

    private void removerAgente(UUID idEquipo) {
        if (emisores.remove(idEquipo) != null) {
            LOG.debug("Agente {} removido del canal SSE. Conexiones activas: {}", idEquipo, emisores.size());
        }
    }
}
