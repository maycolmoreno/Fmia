package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.modelo.OleadaOrquestacion;
import com.farmamia.operations.dominio.modelo.PlanOrquestacionDespliegue;
import com.farmamia.operations.dominio.modelo.SolicitudPlanOrquestacion;
import com.farmamia.operations.dominio.puerto.RepositorioOrquestacionDespliegues;
import com.farmamia.operations.infraestructura.persistencia.entidad.DespliegueEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.EstadoControlDespliegueEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.OleadaDespliegueEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.DespliegueRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EstadoControlDespliegueRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.OleadaDespliegueRepositorioJpa;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public RepositorioOrquestacionDesplieguesJpaAdaptador(
        DespliegueRepositorioJpa despliegueRepositorioJpa,
        ObjetivoDespliegueRepositorioJpa objetivoRepositorioJpa,
        OleadaDespliegueRepositorioJpa oleadaRepositorioJpa,
        EstadoControlDespliegueRepositorioJpa controlRepositorioJpa
    ) {
        this.despliegueRepositorioJpa = despliegueRepositorioJpa;
        this.objetivoRepositorioJpa = objetivoRepositorioJpa;
        this.oleadaRepositorioJpa = oleadaRepositorioJpa;
        this.controlRepositorioJpa = controlRepositorioJpa;
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
                objetivosOleada.size()
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
            MetricasOleada metricas = metricasOleada(oleada);
            if ("RUNNING".equals(oleada.getEstado())
                && control.isPausaAutomaticaHabilitada()
                && metricas.porcentajeFallo().compareTo(control.getPorcentajeMaximoFallo()) > 0
            ) {
                oleada.pausar();
                control.pausar("Umbral de fallos superado en oleada " + oleada.getNumero());
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
        MetricasOleada metricas = metricasOleada(oleada);
        if (metricas.farmaciasTurno() > 0) {
            throw new IllegalArgumentException(
                "La oleada contiene farmacias de turno. No se puede iniciar sin excepcion operacional explicita."
            );
        }
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
                .thenComparing(objetivo -> objetivo.getEquipo().getSucursal().getCodigo())
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
            oleadas.stream().map(this::aDominio).toList()
        );
    }

    private OleadaOrquestacion aDominio(OleadaDespliegueEntidad oleada) {
        MetricasOleada metricas = metricasOleada(oleada);
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

    private MetricasOleada metricasOleada(OleadaDespliegueEntidad oleada) {
        List<ObjetivoDespliegueEntidad> objetivos = objetivoRepositorioJpa.findByOleada_Id(oleada.getId());
        long completados = objetivos.stream().filter(objetivo -> "COMPLETED".equals(objetivo.getEstado())).count();
        long fallidos = objetivos.stream().filter(objetivo -> ESTADOS_FALLIDOS.contains(objetivo.getEstado())).count();
        long finales = objetivos.stream().filter(objetivo -> ESTADOS_FINALES.contains(objetivo.getEstado())).count();
        long farmaciasTurno = objetivos.stream().filter(objetivo -> objetivo.getEquipo().getSucursal().isDeTurno()).count();
        long pendientes = Math.max(0, objetivos.size() - finales);
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
