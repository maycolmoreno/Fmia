package com.farmamia.posupdate.infraestructura.snmp;

import com.farmamia.posupdate.dominio.modelo.AlertaEquipo;
import com.farmamia.posupdate.dominio.modelo.MetricaEquipo;
import com.farmamia.posupdate.dominio.puerto.RepositorioAlertas;
import com.farmamia.posupdate.dominio.puerto.RepositorioMetricasEquipo;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.TipoEquipo;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "farmamia.snmp", name = "enabled", havingValue = "true")
public class SnmpPollingService {

    private static final Logger LOG = LoggerFactory.getLogger(SnmpPollingService.class);

    private static final String OID_CPU       = "1.3.6.1.4.1.14988.1.1.3.10.0";
    private static final String OID_RAM_USED  = "1.3.6.1.2.1.25.2.3.1.6.65536";
    private static final String OID_RAM_TOTAL = "1.3.6.1.2.1.25.2.3.1.5.65536";
    private static final String OID_SYS_UPTIME = "1.3.6.1.2.1.1.3.0";
    private static final String OID_SYS_DESC   = "1.3.6.1.2.1.1.1.0";

    // ifXTable (64-bit) — prefijos; se concatena ".{ifIndex}" en tiempo de ejecución.
    // Usar siempre HC (High Capacity) para evitar desbordamiento en enlaces de alta velocidad.
    private static final String OID_HC_IN_OCTETS_PREFIX  = "1.3.6.1.2.1.31.1.1.1.6.";   // ifHCInOctets
    private static final String OID_HC_OUT_OCTETS_PREFIX = "1.3.6.1.2.1.31.1.1.1.10.";  // ifHCOutOctets

    // Fallback 32-bit (ifTable) — solo si el dispositivo no soporta ifXTable.
    private static final String OID_IN_OCTETS_PREFIX  = "1.3.6.1.2.1.2.2.1.10.";  // ifInOctets
    private static final String OID_OUT_OCTETS_PREFIX = "1.3.6.1.2.1.2.2.1.16.";  // ifOutOctets

    // Límite del contador 32-bit para corrección de desbordamiento.
    private static final long MAX_COUNTER32 = 0x1_0000_0000L; // 2^32

    private final EquipoRepositorioJpa equipos;
    private final RepositorioMetricasEquipo metricas;
    private final RepositorioAlertas alertas;
    private final Map<UUID, Integer> lecturasCpuAltas = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> alertaCpuEmitida = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> alertaRamEmitida = new ConcurrentHashMap<>();
    private final Map<UUID, ContadoresTrafico> traficoAnterior = new ConcurrentHashMap<>();

    @Value("${farmamia.snmp.community:public}")
    private String comunidad;

    @Value("${farmamia.snmp.port:161}")
    private int puerto;

    @Value("${farmamia.snmp.timeout-ms:1500}")
    private long timeoutMs;

    @Value("${farmamia.snmp.retries:1}")
    private int reintentos;

    public SnmpPollingService(
        EquipoRepositorioJpa equipos,
        RepositorioMetricasEquipo metricas,
        RepositorioAlertas alertas
    ) {
        this.equipos = equipos;
        this.metricas = metricas;
        this.alertas = alertas;
    }

    @Scheduled(initialDelay = 0, fixedDelayString = "${farmamia.snmp.fixed-delay-ms:30000}")
    @Transactional
    public void recolectarHardwareMikrotik() {
        LOG.info("====== [NOC SNMP] Iniciando ciclo de lectura proactiva ======");
        for (EquipoEntidad equipo : equipos.findByTipoAndDireccionIpIsNotNullOrderByNombreEquipoAsc(TipoEquipo.NETWORK_LINK)) {
            try {
                LOG.info("[NOC SNMP] Consultando -> PDV: {} | IP: {} | ifIndex: {} | Comunidad: {}",
                    equipo.getCodigoPdv(), equipo.getDireccionIp(), equipo.getSnmpIfIndex(), equipo.getComunidadSnmp());
                LecturaHardware lectura = leerHardware(equipo.getId(), equipo.getDireccionIp(), comunidadEquipo(equipo), equipo.getSnmpIfIndex());
                metricas.guardar(new MetricaEquipo(
                    equipo.getId(),
                    equipo.getVersionPos(),
                    null,
                    null,
                    null,
                    null,
                    BigDecimal.ZERO,
                    "SNMP",
                    lectura.usoCpuPorcentaje(),
                    lectura.usoRamPorcentaje(),
                    lectura.tiempoRespuestaMs(),
                    lectura.traficoInboundKbps(),
                    lectura.traficoOutboundKbps(),
                    lectura.uptimeRouterTicks(),
                    lectura.descripcionRouter()
                ));
                equipo.registrarLatido(equipo.getVersionPos());
                LOG.info("[NOC SNMP]  CONEXIÓN EXITOSA con {} ({}) - Estado actualizado a ONLINE", equipo.getCodigoPdv(), equipo.getDireccionIp());
                evaluarAlertas(equipo, lectura);
            } catch (RuntimeException | IOException ex) {
                LOG.warn("[NOC SNMP] ❌ ERROR/TIMEOUT al conectar con {} ({}). Detalles: {}", equipo.getCodigoPdv(), equipo.getDireccionIp(), ex.getMessage());
            }
        }
    }

    private LecturaHardware leerHardware(UUID idEquipo, String ip, String comunidadSnmp, int ifIndex) throws IOException {
        try (TransportMapping<UdpAddress> transporte = new DefaultUdpTransportMapping();
             Snmp snmp = new Snmp(transporte)) {
            transporte.listen();
            long inicio = System.nanoTime();

            Integer cpu      = getEntero(snmp, ip, comunidadSnmp, OID_CPU);
            Integer ramUsada = getEntero(snmp, ip, comunidadSnmp, OID_RAM_USED);
            Integer ramTotal = getEntero(snmp, ip, comunidadSnmp, OID_RAM_TOTAL);
            Long uptimeTicks = getLong(snmp, ip, comunidadSnmp, OID_SYS_UPTIME);
            String descripcion = getTexto(snmp, ip, comunidadSnmp, OID_SYS_DESC);

            // Intentar contadores HC 64-bit primero; si el dispositivo no los soporta, caer a 32-bit.
            Long inboundBytes  = getLong(snmp, ip, comunidadSnmp, OID_HC_IN_OCTETS_PREFIX  + ifIndex);
            Long outboundBytes = getLong(snmp, ip, comunidadSnmp, OID_HC_OUT_OCTETS_PREFIX + ifIndex);
            boolean es64bit = inboundBytes != null && outboundBytes != null;

            if (!es64bit) {
                LOG.debug("[NOC SNMP] ifHCOctets no disponible en {} ifIndex={}, usando ifOctets 32-bit", ip, ifIndex);
                inboundBytes  = getLong(snmp, ip, comunidadSnmp, OID_IN_OCTETS_PREFIX  + ifIndex);
                outboundBytes = getLong(snmp, ip, comunidadSnmp, OID_OUT_OCTETS_PREFIX + ifIndex);
            }

            int tiempoRespuestaMs = (int) Math.max(0, (System.nanoTime() - inicio) / 1_000_000);
            TraficoKbps trafico = calcularTrafico(idEquipo, inboundBytes, outboundBytes, Instant.now(), es64bit);

            LOG.debug("[NOC SNMP] {} ifIndex={} | in={} out={} ({}bit) | trafico={}/{}",
                ip, ifIndex, inboundBytes, outboundBytes, es64bit ? 64 : 32,
                trafico.inboundKbps(), trafico.outboundKbps());

            return new LecturaHardware(
                cpu,
                calcularPorcentajeRam(ramUsada, ramTotal),
                tiempoRespuestaMs,
                trafico.inboundKbps(),
                trafico.outboundKbps(),
                uptimeTicks,
                descripcion
            );
        }
    }

    private Integer getEntero(Snmp snmp, String ip, String comunidadSnmp, String oid) throws IOException {
        PDU pdu = new PDU();
        pdu.setType(PDU.GET);
        pdu.add(new VariableBinding(new OID(oid)));

        ResponseEvent<Address> respuesta = snmp.get(pdu, destino(ip, comunidadSnmp));
        if (respuesta == null || respuesta.getResponse() == null || respuesta.getResponse().size() == 0) {
            return null;
        }

        Variable variable = respuesta.getResponse().get(0).getVariable();
        if (variable == null || variable.isException()) {
            return null;
        }
        return variable.toInt();
    }

    private Long getLong(Snmp snmp, String ip, String comunidadSnmp, String oid) throws IOException {
        PDU pdu = new PDU();
        pdu.setType(PDU.GET);
        pdu.add(new VariableBinding(new OID(oid)));

        ResponseEvent<Address> respuesta = snmp.get(pdu, destino(ip, comunidadSnmp));
        if (respuesta == null || respuesta.getResponse() == null || respuesta.getResponse().size() == 0) {
            return null;
        }

        Variable variable = respuesta.getResponse().get(0).getVariable();
        if (variable == null || variable.isException()) {
            return null;
        }
        return variable.toLong();
    }

    private String getTexto(Snmp snmp, String ip, String comunidadSnmp, String oid) throws IOException {
        PDU pdu = new PDU();
        pdu.setType(PDU.GET);
        pdu.add(new VariableBinding(new OID(oid)));

        ResponseEvent<Address> respuesta = snmp.get(pdu, destino(ip, comunidadSnmp));
        if (respuesta == null || respuesta.getResponse() == null || respuesta.getResponse().size() == 0) {
            return null;
        }

        Variable variable = respuesta.getResponse().get(0).getVariable();
        if (variable == null || variable.isException()) {
            return null;
        }
        return variable.toString();
    }

    private CommunityTarget<Address> destino(String ip, String comunidadSnmp) {
        CommunityTarget<Address> target = new CommunityTarget<>();
        target.setCommunity(new OctetString(comunidadSnmp));
        target.setAddress(GenericAddress.parse("udp:" + ip + "/" + puerto));
        target.setVersion(SnmpConstants.version2c);
        target.setTimeout(timeoutMs);
        target.setRetries(reintentos);
        return target;
    }

    private String comunidadEquipo(EquipoEntidad equipo) {
        return equipo.getComunidadSnmp() == null || equipo.getComunidadSnmp().isBlank()
            ? comunidad
            : equipo.getComunidadSnmp();
    }

    private void evaluarAlertas(EquipoEntidad equipo, LecturaHardware lectura) {
        if (debeAlertarCpu(equipo.getId(), lectura.usoCpuPorcentaje())) {
            alertas.guardar(new AlertaEquipo(
                equipo.getId(),
                "CRITICAL",
                "HIGH_CPU_USAGE",
                "CPU alta en " + equipo.getNombreEquipo(),
                "CPU SNMP sobre 90% durante dos lecturas consecutivas."
            ));
        }

        if (debeAlertarRam(equipo.getId(), lectura.usoRamPorcentaje())) {
            alertas.guardar(new AlertaEquipo(
                equipo.getId(),
                "WARNING",
                "LOW_MEMORY",
                "Memoria baja en " + equipo.getNombreEquipo(),
                "RAM SNMP sobre 95%."
            ));
        }
    }

    boolean debeAlertarCpu(UUID idEquipo, Integer usoCpuPorcentaje) {
        if (usoCpuPorcentaje == null || usoCpuPorcentaje <= 90) {
            lecturasCpuAltas.remove(idEquipo);
            alertaCpuEmitida.remove(idEquipo);
            return false;
        }

        int consecutivas = lecturasCpuAltas.merge(idEquipo, 1, Integer::sum);
        if (consecutivas < 2 || Boolean.TRUE.equals(alertaCpuEmitida.get(idEquipo))) {
            return false;
        }
        alertaCpuEmitida.put(idEquipo, true);
        return true;
    }

    boolean debeAlertarRam(UUID idEquipo, Integer usoRamPorcentaje) {
        if (usoRamPorcentaje == null || usoRamPorcentaje <= 95) {
            alertaRamEmitida.remove(idEquipo);
            return false;
        }
        return alertaRamEmitida.put(idEquipo, true) == null;
    }

    static Integer calcularPorcentajeRam(Integer ramUsada, Integer ramTotal) {
        if (ramUsada == null || ramTotal == null || ramTotal <= 0) {
            return null;
        }
        return Math.clamp(Math.round(ramUsada * 100f / ramTotal), 0, 100);
    }

    TraficoKbps calcularTrafico(UUID idEquipo, Long inboundBytes, Long outboundBytes, Instant ahora, boolean es64bit) {
        if (idEquipo == null || inboundBytes == null || outboundBytes == null) {
            return new TraficoKbps(null, null);
        }

        ContadoresTrafico actual = new ContadoresTrafico(inboundBytes, outboundBytes, ahora, es64bit);
        ContadoresTrafico anterior = traficoAnterior.put(idEquipo, actual);

        // Primera lectura — no hay delta todavía; el siguiente ciclo ya tendrá valor.
        if (anterior == null) {
            LOG.debug("[NOC SNMP] Primera lectura para equipo {} — tráfico disponible en el próximo ciclo", idEquipo);
            return new TraficoKbps(null, null);
        }

        long segundos = Math.max(1, ahora.getEpochSecond() - anterior.instante().getEpochSecond());
        long deltaIn  = deltaBytesConWrap(inboundBytes,  anterior.inboundBytes(),  anterior.es64bit());
        long deltaOut = deltaBytesConWrap(outboundBytes, anterior.outboundBytes(), anterior.es64bit());

        return new TraficoKbps(
            kbps(deltaIn,  segundos),
            kbps(deltaOut, segundos)
        );
    }

    /**
     * Calcula delta de bytes corrigiendo desbordamiento de contadores.
     * Contador 32-bit desborda en ~4.3 GB; a 7.5 Mbps ocurre cada ~80 min.
     * Contador 64-bit desborda en ~18.4 exabytes — prácticamente imposible.
     */
    private long deltaBytesConWrap(long actual, long anterior, boolean es64bit) {
        long delta = actual - anterior;
        if (delta >= 0) {
            return delta;
        }
        // El contador dio vuelta. Para 32-bit corregir sumando 2^32.
        // Para 64-bit un delta negativo indica un reinicio del router → descartar.
        if (!es64bit) {
            LOG.debug("[NOC SNMP] Desbordamiento Counter32 detectado (delta={}). Aplicando corrección wrap.", delta);
            return delta + MAX_COUNTER32;
        }
        LOG.warn("[NOC SNMP] Delta negativo en Counter64 (delta={}) — posible reinicio del router. Descartando ciclo.", delta);
        return 0L;
    }

    private BigDecimal kbps(long deltaBytes, long segundos) {
        if (deltaBytes <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(deltaBytes)
            .multiply(BigDecimal.valueOf(8))
            .divide(BigDecimal.valueOf(segundos * 1000L), 2, RoundingMode.HALF_UP);
    }

    record LecturaHardware(
        Integer usoCpuPorcentaje,
        Integer usoRamPorcentaje,
        Integer tiempoRespuestaMs,
        BigDecimal traficoInboundKbps,
        BigDecimal traficoOutboundKbps,
        Long uptimeRouterTicks,
        String descripcionRouter
    ) {
    }

    record ContadoresTrafico(Long inboundBytes, Long outboundBytes, Instant instante, boolean es64bit) {
    }

    record TraficoKbps(BigDecimal inboundKbps, BigDecimal outboundKbps) {
    }
}
