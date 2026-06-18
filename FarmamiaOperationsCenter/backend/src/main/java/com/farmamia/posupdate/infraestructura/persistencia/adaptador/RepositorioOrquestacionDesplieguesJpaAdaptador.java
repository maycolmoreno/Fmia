package com.farmamia.posupdate.infraestructura.persistencia.adaptador;

import com.farmamia.posupdate.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.posupdate.dominio.modelo.OleadaOrquestacion;
import com.farmamia.posupdate.dominio.modelo.PlanOrquestacionDespliegue;
import com.farmamia.posupdate.dominio.modelo.SolicitudPlanOrquestacion;
import com.farmamia.posupdate.dominio.puerto.RepositorioOrquestacionDespliegues;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.DespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.EstadoControlDespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.OleadaDespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.DespliegueRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.EstadoControlDespliegueRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.OleadaDespliegueRepositorioJpa;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioOrquestacionDesplieguesJpaAdaptador implements RepositorioOrquestacionDespliegues {

    private static final List<String> ESTADOS_FALLIDOS = List.of("FAILED", "ROLLBACK_FAILED");
    private static final List<String> ESTADOS_FINALES = List.of(
        "COMPLETED",
        "FAILED",
        "ROLLBACK_COMPLETED",
        "ROLLBACK_FAILED",
        "SKIPPED"
    );

    private final DespliegueRepositorioJpa despliegueRepositorioJpa;
    private final ObjetivoDespliegueRepositorioJpa objetivoRepositorioJpa;
    private final OleadaDespliegueRepositorioJpa oleadaRepositorioJpa;
    private final EstadoControlDespliegueRepositorioJpa controlRepositorioJpa;
    private final Counter reintentosReautorizados;
    private final Counter pausasAutomaticas;

    public RepositorioOrquestacionDesplieguesJpaAdaptador(
        DespliegueRepositorioJpa despliegueRepositorioJpa,
        ObjetivoDespliegueRepositorioJpa objetivoRepositorioJpa,
        OleadaDespliegueRepositorioJpa oleadaRepositorioJpa,
        EstadoControlDespliegueRepositorioJpa controlRepositorioJpa,
        MeterRegistry meterRegistry
    ) {
        this.despliegueRepositorioJpa = despliegueRepositorioJpa;
        this.objetivoRepositorioJpa = objetivoRepositorioJpa;
        this.oleadaRepositorioJpa = oleadaRepositorioJpa;
        this.controlRepositorioJpa = controlRepositorioJpa;
        this.reintentosReautorizados = Counter.builder("farmamia.orchestration.retries.reauthorized.total")
            .description("Objetivos fallidos reautorizados por la evaluacion de orquestacion")
            .register(meterRegistry);
        this.pausasAutomaticas = Counter.builder("farmamia.orchestration.auto.pauses.total")
            .description("Pausas automaticas de campanas por umbral de fallos")
            .register(meterRegistry);
    }

    @Override
    public PlanOrquestacionDespliegue planificar(UUID idDespliegue, SolicitudPlanOrquestacion solicitud) {
        DespliegueEntidad despliegue = buscarDespliegue(idDespliegue);
        List<ObjetivoDespliegueEntidad> objetivos = objetivoRepositorioJpa.findByDespliegue_Id(idDespliegue);
        if (objetivos.isEmpty()) {
            throw new IllegalArgumentException("El despliegue no tiene equipos objetivo para orquestar.");
        }

        objetivos.forEach(objetivo -> objetivo.asignarOleada(null));
        objetivoRepositorioJpa.saveAll(objetivos);
        oleadaRepositorioJpa.deleteByDespliegue_Id(idDespliegue);

        List<OleadaDespliegueEntidad> oleadas = new ArrayList<>();
        int numero = 1;
        for (Map.Entry<ClaveOleada, List<ObjetivoDespliegueEntidad>> entrada : agruparObjetivos(objetivos).entrySet()) {
            ClaveOleada clave = entrada.getKey();
            List<ObjetivoDespliegueEntidad> objetivosOleada = entrada.getValue();
            OleadaDespliegueEntidad oleada = oleadaRepositorioJpa.save(new OleadaDespliegueEntidad(
                despliegue,
                numero++,
                clave.nombre(),
                clave.grupoObjetivo(),
                clave.piloto(),
                solicitud.porcentajeMaximoFallo(),
                solicitud.pausaAutomaticaHabilitada(),
                solicitud.ventanaInicio(),
                solicitud.ventanaFin(),
                objetivosOleada.size(),
                solicitud.maximoEquiposParalelos()
            ));
            objetivosOleada.forEach(objetivo -> objetivo.asignarOleada(oleada));
            objetivoRepositorioJpa.saveAll(objetivosOleada);
            oleadas.add(oleada);
        }

        EstadoControlDespliegueEntidad control = controlRepositorioJpa.save(new EstadoControlDespliegueEntidad(
            despliegue,
            solicitud.porcentajeMaximoFallo(),
            solicitud.pausaAutomaticaHabilitada(),
            solicitud.limiteReintentos()
        ));
        return aPlan(idDespliegue, control, oleadas);
    }

    @Override
    public PlanOrquestacionDespliegue obtenerPlan(UUID idDespliegue) {
        buscarDespliegue(idDespliegue);
        EstadoControlDespliegueEntidad control = buscarControl(idDespliegue);
        return aPlan(idDespliegue, control, oleadaRepositorioJpa.findByDespliegue_IdOrderByNumeroAsc(idDespliegue));
    }

    @Override
    public PlanOrquestacionDespliegue evaluar(UUID idDespliegue) {
        EstadoControlDespliegueEntidad control = buscarControl(idDespliegue);
        List<OleadaDespliegueEntidad> oleadas = oleadaRepositorioJpa.findByDespliegue_IdOrderByNumeroAsc(idDespliegue);
        boolean huboPausa = false;

        for (OleadaDespliegueEntidad oleada : oleadas) {
            reautorizarReintentosDisponibles(oleada, control);
            MetricasOleada metricas = metricasOleada(oleada, control.getLimiteReintentos());
            if ("RUNNING".equals(oleada.getEstado())
                && control.isPausaAutomaticaHabilitada()
                && metricas.porcentajeFallo().compareTo(control.getPorcentajeMaximoFallo()) > 0
            ) {
                oleada.pausar();
                control.pausar("Umbral de fallos superado en oleada " + oleada.getNumero());
                pausasAutomaticas.increment();
                huboPausa = true;
            } else if ("RUNNING".equals(oleada.getEstado()) && metricas.pendientes() == 0) {
                if (metricas.fallidos() > 0) {
                    oleada.fallar();
                } else {
                    oleada.completar();
                }
            }
        }

        if (!huboPausa) {
            control.marcarEvaluado();
            if (oleadas.stream().allMatch(oleada -> "COMPLETED".equals(oleada.getEstado()))) {
                control.completar();
            }
        }

        return aPlan(idDespliegue, control, oleadas);
    }

    @Override
    public PlanOrquestacionDespliegue iniciarOleada(UUID idDespliegue, UUID idOleada) {
        EstadoControlDespliegueEntidad control = buscarControl(idDespliegue);
        OleadaDespliegueEntidad oleada = buscarOleada(idDespliegue, idOleada);
        MetricasOleada metricas = metricasOleada(oleada, control.getLimiteReintentos());
        if (metricas.farmaciasTurno() > 0) {
            throw new IllegalArgumentException(
                "La oleada contiene farmacias de turno. No se puede iniciar sin excepcion operacional explicita."
            );
        }
        List<ObjetivoDespliegueEntidad> objetivos = objetivoRepositorioJpa.findByOleada_Id(oleada.getId());
        objetivos.forEach(ObjetivoDespliegueEntidad::autorizarDesdeOleada);
        objetivoRepositorioJpa.saveAll(objetivos);
        oleada.iniciar();
        control.correr(oleada.getNumero() + 1);
        return aPlan(idDespliegue, control, oleadaRepositorioJpa.findByDespliegue_IdOrderByNumeroAsc(idDespliegue));
    }

    @Override
    public PlanOrquestacionDespliegue pausarOleada(UUID idDespliegue, UUID idOleada) {
        EstadoControlDespliegueEntidad control = buscarControl(idDespliegue);
        OleadaDespliegueEntidad oleada = buscarOleada(idDespliegue, idOleada);
        oleada.pausar();
        control.pausar("Pausa manual de oleada " + oleada.getNumero());
        return aPlan(idDespliegue, control, oleadaRepositorioJpa.findByDespliegue_IdOrderByNumeroAsc(idDespliegue));
    }

    @Override
    public PlanOrquestacionDespliegue reanudarOleada(UUID idDespliegue, UUID idOleada) {
        return iniciarOleada(idDespliegue, idOleada);
    }

    private Map<ClaveOleada, List<ObjetivoDespliegueEntidad>> agruparObjetivos(List<ObjetivoDespliegueEntidad> objetivos) {
        return objetivos.stream()
            .sorted(Comparator
                .comparing(ObjetivoDespliegueEntidad::isPiloto).reversed()
                .thenComparing(objetivo -> grupoNormalizado(objetivo.getGrupoObjetivo()))
                .thenComparing(objetivo -> codigoSucursal(objetivo.getEquipo()))
                .thenComparing(objetivo -> objetivo.getEquipo().getNombreEquipo()))
            .collect(Collectors.groupingBy(
                objetivo -> new ClaveOleada(
                    objetivo.isPiloto(),
                    grupoNormalizado(objetivo.getGrupoObjetivo()),
                    objetivo.isPiloto()
                        ? "Piloto controlado"
                        : "Oleada " + grupoNormalizado(objetivo.getGrupoObjetivo())
                ),
                LinkedHashMap::new,
                Collectors.toList()
            ));
    }

    private PlanOrquestacionDespliegue aPlan(
        UUID idDespliegue,
        EstadoControlDespliegueEntidad control,
        List<OleadaDespliegueEntidad> oleadas
    ) {
        return new PlanOrquestacionDespliegue(
            idDespliegue,
            control.getEstado(),
            control.getPorcentajeMaximoFallo(),
            control.isPausaAutomaticaHabilitada(),
            control.getLimiteReintentos(),
            control.getSiguienteNumeroOleada(),
            control.getMotivoPausa(),
            control.getEvaluadoEn(),
            oleadas.stream().map(oleada -> aDominio(oleada, control.getLimiteReintentos())).toList()
        );
    }

    private OleadaOrquestacion aDominio(OleadaDespliegueEntidad oleada, int limiteReintentos) {
        MetricasOleada metricas = metricasOleada(oleada, limiteReintentos);
        return new OleadaOrquestacion(
            oleada.getId(),
            oleada.getNumero(),
            oleada.getNombre(),
            oleada.getGrupoObjetivo(),
            oleada.isPiloto(),
            oleada.getEstado(),
            oleada.getObjetivosPlanificados(),
            metricas.completados(),
            metricas.fallidos(),
            metricas.pendientes(),
            metricas.farmaciasTurno(),
            metricas.porcentajeFallo().doubleValue(),
            oleada.getVentanaInicio(),
            oleada.getVentanaFin(),
            oleada.getIniciadoEn(),
            oleada.getCompletadoEn()
        );
    }

    private void reautorizarReintentosDisponibles(
        OleadaDespliegueEntidad oleada,
        EstadoControlDespliegueEntidad control
    ) {
        if (!"RUNNING".equals(oleada.getEstado())) {
            return;
        }
        OffsetDateTime ahora = OffsetDateTime.now();
        List<ObjetivoDespliegueEntidad> objetivos = objetivoRepositorioJpa.findByOleada_Id(oleada.getId());
        List<ObjetivoDespliegueEntidad> reintentos = objetivos.stream()
            .filter(objetivo -> objetivo.reintentoDisponible(ahora, control.getLimiteReintentos()))
            .peek(ObjetivoDespliegueEntidad::autorizarReintento)
            .toList();
        if (!reintentos.isEmpty()) {
            objetivoRepositorioJpa.saveAll(reintentos);
            reintentosReautorizados.increment(reintentos.size());
        }
    }

    private MetricasOleada metricasOleada(OleadaDespliegueEntidad oleada, int limiteReintentos) {
        List<ObjetivoDespliegueEntidad> objetivos = objetivoRepositorioJpa.findByOleada_Id(oleada.getId());
        long completados = objetivos.stream().filter(objetivo -> "COMPLETED".equals(objetivo.getEstado())).count();
        long fallidos = objetivos.stream().filter(objetivo -> ESTADOS_FALLIDOS.contains(objetivo.getEstado())).count();
        long finales = objetivos.stream().filter(objetivo -> ESTADOS_FINALES.contains(objetivo.getEstado())).count();
        long fallidosConReintentoPendiente = objetivos.stream()
            .filter(ObjetivoDespliegueEntidad::esFalloReintentable)
            .filter(objetivo -> !objetivo.reintentosAgotados(limiteReintentos))
            .count();
        long farmaciasTurno = objetivos.stream()
            .filter(objetivo -> objetivo.getEquipo().getSucursal() != null)
            .filter(objetivo -> objetivo.getEquipo().getSucursal().isDeTurno())
            .count();
        long pendientes = Math.max(0, objetivos.size() - finales + fallidosConReintentoPendiente);
        BigDecimal porcentajeFallo = objetivos.isEmpty()
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(fallidos * 100.0 / objetivos.size()).setScale(2, RoundingMode.HALF_UP);
        return new MetricasOleada(completados, fallidos, pendientes, farmaciasTurno, porcentajeFallo);
    }

    private DespliegueEntidad buscarDespliegue(UUID idDespliegue) {
        return despliegueRepositorioJpa.findById(idDespliegue)
            .orElseThrow(() -> new RecursoNoEncontradoException("Despliegue no encontrado: " + idDespliegue));
    }

    private EstadoControlDespliegueEntidad buscarControl(UUID idDespliegue) {
        return controlRepositorioJpa.findById(idDespliegue)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "El despliegue no tiene plan de orquestacion. Ejecuta /plan primero."
            ));
    }

    private OleadaDespliegueEntidad buscarOleada(UUID idDespliegue, UUID idOleada) {
        return oleadaRepositorioJpa.findByIdAndDespliegue_Id(idOleada, idDespliegue)
            .orElseThrow(() -> new RecursoNoEncontradoException("Oleada no encontrada: " + idOleada));
    }

    private String grupoNormalizado(String grupo) {
        return grupo == null || grupo.isBlank() ? "GENERAL" : grupo.trim().toUpperCase();
    }

    private String codigoSucursal(com.farmamia.posupdate.infraestructura.persistencia.entidad.EquipoEntidad equipo) {
        return equipo.getSucursal() == null ? "" : equipo.getSucursal().getCodigo();
    }

    private record ClaveOleada(boolean piloto, String grupoObjetivo, String nombre) {
    }

    private record MetricasOleada(
        long completados,
        long fallidos,
        long pendientes,
        long farmaciasTurno,
        BigDecimal porcentajeFallo
    ) {
    }
}
