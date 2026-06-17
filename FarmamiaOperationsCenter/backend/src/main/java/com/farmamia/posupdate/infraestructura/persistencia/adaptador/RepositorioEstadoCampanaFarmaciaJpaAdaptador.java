package com.farmamia.posupdate.infraestructura.persistencia.adaptador;

import com.farmamia.posupdate.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.posupdate.dominio.modelo.EquipoEstadoCampanaFarmacia;
import com.farmamia.posupdate.dominio.modelo.EstadoCampanaFarmacia;
import com.farmamia.posupdate.dominio.modelo.EstadoOperacionalCampanaFarmacia;
import com.farmamia.posupdate.dominio.modelo.EstadoTecnicoCampanaFarmacia;
import com.farmamia.posupdate.dominio.modelo.FiltroEstadoCampanaFarmacia;
import com.farmamia.posupdate.dominio.modelo.ResumenEstadoCampanaFarmacia;
import com.farmamia.posupdate.dominio.puerto.RepositorioEstadoCampanaFarmacia;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.AlertaEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.DespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.GrupoTrxEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.SucursalEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.AlertaRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.DespliegueRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.GrupoTrxRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioEstadoCampanaFarmaciaJpaAdaptador implements RepositorioEstadoCampanaFarmacia {

    private static final List<String> ESTADOS_ALERTA_ABIERTA = List.of("OPEN", "ACKNOWLEDGED");
    private static final List<String> ESTADOS_FINALES = List.of("COMPLETED", "FAILED", "ROLLBACK_COMPLETED", "ROLLBACK_FAILED", "SKIPPED");
    private static final List<String> ESTADOS_SIN_INICIO = List.of("PENDING", "AUTHORIZED", "WAITING_WINDOW", "WAITING_ACTIVITY", "OFFLINE");

    private final DespliegueRepositorioJpa despliegueRepositorioJpa;
    private final ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa;
    private final AlertaRepositorioJpa alertaRepositorioJpa;
    private final GrupoTrxRepositorioJpa grupoTrxRepositorioJpa;

    public RepositorioEstadoCampanaFarmaciaJpaAdaptador(
        DespliegueRepositorioJpa despliegueRepositorioJpa,
        ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa,
        AlertaRepositorioJpa alertaRepositorioJpa,
        GrupoTrxRepositorioJpa grupoTrxRepositorioJpa
    ) {
        this.despliegueRepositorioJpa = despliegueRepositorioJpa;
        this.objetivoDespliegueRepositorioJpa = objetivoDespliegueRepositorioJpa;
        this.alertaRepositorioJpa = alertaRepositorioJpa;
        this.grupoTrxRepositorioJpa = grupoTrxRepositorioJpa;
    }

    @Override
    public ResumenEstadoCampanaFarmacia consultar(UUID idCampana, FiltroEstadoCampanaFarmacia filtro) {
        DespliegueEntidad campana = despliegueRepositorioJpa.findById(idCampana)
            .orElseThrow(() -> new RecursoNoEncontradoException("Campana POS no encontrada: " + idCampana));

        List<ObjetivoDespliegueEntidad> objetivos = objetivoDespliegueRepositorioJpa.findByDespliegue_Id(idCampana);
        Map<UUID, AlertasFarmacia> alertasPorFarmacia = alertasPorFarmacia();
        Map<String, GrupoTrxEntidad> gruposPorCodigo = grupoTrxRepositorioJpa.findAll()
            .stream()
            .collect(Collectors.toMap(grupo -> grupo.getCodigo().toLowerCase(Locale.ROOT), Function.identity()));

        List<EstadoCampanaFarmacia> estados = agruparPorFarmacia(objetivos)
            .values()
            .stream()
            .map(grupo -> calcularEstado(campana, grupo, alertasPorFarmacia, gruposPorCodigo))
            .filter(estado -> aplicaFiltro(estado, filtro))
            .sorted(comparador(filtro.orden()))
            .toList();

        List<EstadoCampanaFarmacia> todos = agruparPorFarmacia(objetivos)
            .values()
            .stream()
            .map(grupo -> calcularEstado(campana, grupo, alertasPorFarmacia, gruposPorCodigo))
            .sorted(comparador("prioridad,asc"))
            .toList();

        int desde = Math.min(filtro.pagina() * filtro.tamano(), estados.size());
        int hasta = Math.min(desde + filtro.tamano(), estados.size());
        List<EstadoCampanaFarmacia> pagina = estados.subList(desde, hasta);
        int totalPaginas = estados.isEmpty() ? 0 : (int) Math.ceil((double) estados.size() / filtro.tamano());

        return new ResumenEstadoCampanaFarmacia(
            campana.getId(),
            campana.getNombre(),
            campana.getPaquete().getVersion(),
            campana.getEstado(),
            todos.size(),
            contarTecnico(todos, EstadoTecnicoCampanaFarmacia.COMPLETADA),
            contarTecnico(todos, EstadoTecnicoCampanaFarmacia.PENDIENTE),
            contarTecnico(todos, EstadoTecnicoCampanaFarmacia.EN_PROGRESO),
            contarConErrores(todos),
            contarOperacional(todos, EstadoOperacionalCampanaFarmacia.EN_RIESGO),
            contarOperacional(todos, EstadoOperacionalCampanaFarmacia.CRITICA),
            (int) todos.stream().filter(estado -> estado.deTurno() && estado.estadoOperacional() != EstadoOperacionalCampanaFarmacia.NORMAL).count(),
            porcentajeAvance(todos),
            porcentajeExito(todos),
            peorGrupo(todos),
            filtro.pagina(),
            filtro.tamano(),
            estados.size(),
            totalPaginas,
            hasta < estados.size(),
            pagina
        );
    }

    private Map<UUID, List<ObjetivoDespliegueEntidad>> agruparPorFarmacia(List<ObjetivoDespliegueEntidad> objetivos) {
        Map<UUID, List<ObjetivoDespliegueEntidad>> grupos = new LinkedHashMap<>();
        for (ObjetivoDespliegueEntidad objetivo : objetivos) {
            UUID farmaciaId = objetivo.getEquipo().getSucursal().getId();
            grupos.computeIfAbsent(farmaciaId, id -> new ArrayList<>()).add(objetivo);
        }
        return grupos;
    }

    private EstadoCampanaFarmacia calcularEstado(
        DespliegueEntidad campana,
        List<ObjetivoDespliegueEntidad> objetivos,
        Map<UUID, AlertasFarmacia> alertasPorFarmacia,
        Map<String, GrupoTrxEntidad> gruposPorCodigo
    ) {
        ObjetivoDespliegueEntidad primero = objetivos.get(0);
        SucursalEntidad farmacia = primero.getEquipo().getSucursal();
        AlertasFarmacia alertas = alertasPorFarmacia.getOrDefault(farmacia.getId(), new AlertasFarmacia(0, 0));
        int total = objetivos.size();
        int completados = contar(objetivos, "COMPLETED");
        int rollbacks = (int) objetivos.stream().filter(objetivo -> esRollback(objetivo.getEstado())).count();
        int fallidos = (int) objetivos.stream().filter(objetivo -> "FAILED".equals(objetivo.getEstado()) || "ROLLBACK_FAILED".equals(objetivo.getEstado())).count();
        int pendientes = (int) objetivos.stream().filter(objetivo -> !ESTADOS_FINALES.contains(objetivo.getEstado())).count();
        boolean todosSinInicio = objetivos.stream().allMatch(objetivo -> ESTADOS_SIN_INICIO.contains(objetivo.getEstado()));
        boolean hayOffline = objetivos.stream().anyMatch(objetivo -> "OFFLINE".equals(objetivo.getEstado()) || "OFFLINE".equals(objetivo.getEquipo().getEstado()));
        boolean grupoPausado = objetivos.stream().map(ObjetivoDespliegueEntidad::getGrupoObjetivo)
            .filter(Objects::nonNull)
            .map(valor -> valor.toLowerCase(Locale.ROOT))
            .map(gruposPorCodigo::get)
            .filter(Objects::nonNull)
            .anyMatch(grupo -> "PAUSADO".equals(grupo.getEstado()));

        EstadoTecnicoCampanaFarmacia estadoTecnico = estadoTecnico(total, completados, pendientes, fallidos, rollbacks, todosSinInicio);
        EstadoOperacionalCampanaFarmacia estadoOperacional = estadoOperacional(
            farmacia.isDeTurno(),
            estadoTecnico,
            alertas,
            fallidos,
            rollbacks,
            hayOffline,
            grupoPausado
        );
        String codigoGrupo = codigoGrupo(objetivos);
        GrupoTrxEntidad grupoTrx = codigoGrupo == null || codigoGrupo.contains(",")
            ? null
            : gruposPorCodigo.get(codigoGrupo.toLowerCase(Locale.ROOT));

        return new EstadoCampanaFarmacia(
            farmacia.getId(),
            farmacia.getCodigo(),
            farmacia.getNombre(),
            campana.getId(),
            grupoTrx == null ? null : grupoTrx.getId(),
            codigoGrupo,
            farmacia.isDeTurno(),
            total,
            completados,
            pendientes,
            fallidos,
            rollbacks,
            ultimoHeartbeat(objetivos),
            alertas.criticas(),
            alertas.abiertas(),
            estadoTecnico,
            estadoOperacional,
            resumenRiesgo(estadoOperacional, estadoTecnico, farmacia.isDeTurno(), alertas, fallidos, rollbacks, hayOffline, grupoPausado),
            objetivos.stream().map(this::aEquipo).toList()
        );
    }

    private EstadoTecnicoCampanaFarmacia estadoTecnico(int total, int completados, int pendientes, int fallidos, int rollbacks, boolean todosSinInicio) {
        if (total == 0 || todosSinInicio) {
            return EstadoTecnicoCampanaFarmacia.PENDIENTE;
        }
        if (fallidos > 0 || (completados == 0 && rollbacks > 0)) {
            return EstadoTecnicoCampanaFarmacia.FALLIDA;
        }
        if (pendientes == 0 && rollbacks > 0) {
            return EstadoTecnicoCampanaFarmacia.COMPLETADA_CON_FALLOS;
        }
        if (pendientes == 0 && completados == total) {
            return EstadoTecnicoCampanaFarmacia.COMPLETADA;
        }
        return EstadoTecnicoCampanaFarmacia.EN_PROGRESO;
    }

    private EstadoOperacionalCampanaFarmacia estadoOperacional(
        boolean deTurno,
        EstadoTecnicoCampanaFarmacia estadoTecnico,
        AlertasFarmacia alertas,
        int fallidos,
        int rollbacks,
        boolean hayOffline,
        boolean grupoPausado
    ) {
        if (alertas.criticas() > 0 || estadoTecnico == EstadoTecnicoCampanaFarmacia.FALLIDA || (deTurno && (hayOffline || fallidos > 0))) {
            return EstadoOperacionalCampanaFarmacia.CRITICA;
        }
        if (rollbacks > 0 || alertas.abiertas() > 0 || hayOffline || grupoPausado || deTurno && estadoTecnico != EstadoTecnicoCampanaFarmacia.COMPLETADA) {
            return EstadoOperacionalCampanaFarmacia.EN_RIESGO;
        }
        return EstadoOperacionalCampanaFarmacia.NORMAL;
    }

    private EquipoEstadoCampanaFarmacia aEquipo(ObjetivoDespliegueEntidad objetivo) {
        EquipoEntidad equipo = objetivo.getEquipo();
        return new EquipoEstadoCampanaFarmacia(
            equipo.getId(),
            equipo.getNombreEquipo(),
            equipo.getEstado(),
            objetivo.getEstado(),
            objetivo.getGrupoObjetivo(),
            objetivo.getVersionAnterior(),
            objetivo.getVersionNueva(),
            objetivo.getUltimoError(),
            equipo.getUltimoLatidoEn(),
            esRollback(objetivo.getEstado())
        );
    }

    private boolean aplicaFiltro(EstadoCampanaFarmacia estado, FiltroEstadoCampanaFarmacia filtro) {
        String q = minuscula(filtro.q());
        return (filtro.estadoTecnico() == null || estado.estadoTecnico().name().equalsIgnoreCase(filtro.estadoTecnico()))
            && (filtro.estadoOperacional() == null || estado.estadoOperacional().name().equalsIgnoreCase(filtro.estadoOperacional()))
            && (filtro.grupoTrx() == null || (estado.codigoGrupoTrx() != null && estado.codigoGrupoTrx().toLowerCase(Locale.ROOT).contains(filtro.grupoTrx().toLowerCase(Locale.ROOT))))
            && (filtro.deTurno() == null || estado.deTurno() == filtro.deTurno())
            && (q == null
                || estado.codigoFarmacia().toLowerCase(Locale.ROOT).contains(q)
                || estado.nombreFarmacia().toLowerCase(Locale.ROOT).contains(q));
    }

    private Comparator<EstadoCampanaFarmacia> comparador(String orden) {
        String[] partes = orden == null ? new String[0] : orden.split(",", 2);
        String campo = partes.length > 0 ? partes[0] : "prioridad";
        boolean desc = partes.length > 1 && "desc".equalsIgnoreCase(partes[1]);
        Comparator<EstadoCampanaFarmacia> comparador = switch (campo) {
            case "codigoFarmacia", "branchCode" -> Comparator.comparing(EstadoCampanaFarmacia::codigoFarmacia);
            case "estadoTecnico" -> Comparator.comparing(estado -> estado.estadoTecnico().name());
            case "estadoOperacional" -> Comparator.comparing(estado -> estado.estadoOperacional().name());
            case "grupoTrx" -> Comparator.comparing(estado -> nuloAValor(estado.codigoGrupoTrx()));
            default -> Comparator.comparingInt(this::prioridad);
        };
        return desc ? comparador.reversed() : comparador.thenComparing(EstadoCampanaFarmacia::codigoFarmacia);
    }

    private int prioridad(EstadoCampanaFarmacia estado) {
        if (estado.estadoOperacional() == EstadoOperacionalCampanaFarmacia.CRITICA) {
            return 0;
        }
        if (estado.deTurno() && estado.estadoOperacional() != EstadoOperacionalCampanaFarmacia.NORMAL) {
            return 1;
        }
        if (estado.estadoTecnico() == EstadoTecnicoCampanaFarmacia.FALLIDA
            || estado.estadoTecnico() == EstadoTecnicoCampanaFarmacia.COMPLETADA_CON_FALLOS) {
            return 2;
        }
        if (estado.estadoTecnico() == EstadoTecnicoCampanaFarmacia.PENDIENTE) {
            return 3;
        }
        if (estado.estadoTecnico() == EstadoTecnicoCampanaFarmacia.EN_PROGRESO) {
            return 4;
        }
        return 5;
    }

    private Map<UUID, AlertasFarmacia> alertasPorFarmacia() {
        Map<UUID, AlertasFarmacia> resultado = new HashMap<>();
        for (AlertaEntidad alerta : alertaRepositorioJpa.findByEstadoIn(ESTADOS_ALERTA_ABIERTA)) {
            if (alerta.getEquipo() == null || alerta.getEquipo().getSucursal() == null) {
                continue;
            }
            UUID farmaciaId = alerta.getEquipo().getSucursal().getId();
            AlertasFarmacia actual = resultado.getOrDefault(farmaciaId, new AlertasFarmacia(0, 0));
            int criticas = actual.criticas() + ("CRITICAL".equals(alerta.getSeveridad()) ? 1 : 0);
            resultado.put(farmaciaId, new AlertasFarmacia(actual.abiertas() + 1, criticas));
        }
        return resultado;
    }

    private int contarTecnico(List<EstadoCampanaFarmacia> estados, EstadoTecnicoCampanaFarmacia estadoTecnico) {
        return (int) estados.stream().filter(estado -> estado.estadoTecnico() == estadoTecnico).count();
    }

    private int contarOperacional(List<EstadoCampanaFarmacia> estados, EstadoOperacionalCampanaFarmacia estadoOperacional) {
        return (int) estados.stream().filter(estado -> estado.estadoOperacional() == estadoOperacional).count();
    }

    private int contarConErrores(List<EstadoCampanaFarmacia> estados) {
        return (int) estados.stream()
            .filter(estado -> estado.estadoTecnico() == EstadoTecnicoCampanaFarmacia.FALLIDA
                || estado.estadoTecnico() == EstadoTecnicoCampanaFarmacia.COMPLETADA_CON_FALLOS)
            .count();
    }

    private double porcentajeAvance(List<EstadoCampanaFarmacia> estados) {
        int totalEquipos = estados.stream().mapToInt(EstadoCampanaFarmacia::totalEquiposPos).sum();
        int finalizados = estados.stream().mapToInt(estado -> estado.completados() + estado.fallidos() + estado.rollbacks()).sum();
        return porcentaje(finalizados, totalEquipos);
    }

    private double porcentajeExito(List<EstadoCampanaFarmacia> estados) {
        int totalEquipos = estados.stream().mapToInt(EstadoCampanaFarmacia::totalEquiposPos).sum();
        int completados = estados.stream().mapToInt(EstadoCampanaFarmacia::completados).sum();
        return porcentaje(completados, totalEquipos);
    }

    private double porcentaje(int valor, int total) {
        if (total == 0) {
            return 0;
        }
        return BigDecimal.valueOf((valor * 100.0) / total)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private String peorGrupo(List<EstadoCampanaFarmacia> estados) {
        Map<String, Integer> puntaje = new HashMap<>();
        for (EstadoCampanaFarmacia estado : estados) {
            if (estado.codigoGrupoTrx() == null) {
                continue;
            }
            int valor = switch (estado.estadoOperacional()) {
                case CRITICA -> 5;
                case EN_RIESGO -> 3;
                case NORMAL -> estado.estadoTecnico() == EstadoTecnicoCampanaFarmacia.COMPLETADA ? 0 : 1;
            };
            puntaje.merge(estado.codigoGrupoTrx(), valor, Integer::sum);
        }
        return puntaje.entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    private int contar(List<ObjetivoDespliegueEntidad> objetivos, String estado) {
        return (int) objetivos.stream().filter(objetivo -> estado.equals(objetivo.getEstado())).count();
    }

    private boolean esRollback(String estado) {
        return estado != null && estado.startsWith("ROLLBACK");
    }

    private OffsetDateTime ultimoHeartbeat(List<ObjetivoDespliegueEntidad> objetivos) {
        return objetivos.stream()
            .map(objetivo -> objetivo.getEquipo().getUltimoLatidoEn())
            .filter(Objects::nonNull)
            .max(OffsetDateTime::compareTo)
            .orElse(null);
    }

    private String codigoGrupo(List<ObjetivoDespliegueEntidad> objetivos) {
        List<String> codigos = objetivos.stream()
            .map(ObjetivoDespliegueEntidad::getGrupoObjetivo)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(valor -> !valor.isBlank())
            .distinct()
            .sorted()
            .toList();
        if (codigos.isEmpty()) {
            return null;
        }
        return String.join(",", codigos);
    }

    private String resumenRiesgo(
        EstadoOperacionalCampanaFarmacia estadoOperacional,
        EstadoTecnicoCampanaFarmacia estadoTecnico,
        boolean deTurno,
        AlertasFarmacia alertas,
        int fallidos,
        int rollbacks,
        boolean hayOffline,
        boolean grupoPausado
    ) {
        if (estadoOperacional == EstadoOperacionalCampanaFarmacia.CRITICA) {
            if (alertas.criticas() > 0) {
                return "Alerta critica abierta durante campana POS";
            }
            if (deTurno && hayOffline) {
                return "Farmacia de turno con POS offline";
            }
            return "Campana con fallo operativo en farmacia";
        }
        if (estadoOperacional == EstadoOperacionalCampanaFarmacia.EN_RIESGO) {
            if (grupoPausado) {
                return "Grupo TRX pausado con objetivos de farmacia";
            }
            if (rollbacks > 0) {
                return "Rollback ejecutado en la farmacia";
            }
            if (alertas.abiertas() > 0) {
                return "Alertas abiertas durante la campana";
            }
            if (hayOffline) {
                return "POS offline o sin avance confirmado";
            }
            if (deTurno && estadoTecnico != EstadoTecnicoCampanaFarmacia.COMPLETADA) {
                return "Farmacia de turno con campana pendiente";
            }
        }
        return fallidos > 0 ? "Farmacia con errores en campana" : "Sin riesgo operacional relevante";
    }

    private String minuscula(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase(Locale.ROOT);
    }

    private String nuloAValor(String valor) {
        return valor == null ? "" : valor;
    }

    private record AlertasFarmacia(int abiertas, int criticas) {
    }
}
