package com.farmamia.posupdate.infraestructura.persistencia.adaptador;

import com.farmamia.posupdate.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.posupdate.dominio.modelo.CampanaGrupoTrx;
import com.farmamia.posupdate.dominio.modelo.EquipoEstadoCampanaFarmacia;
import com.farmamia.posupdate.dominio.modelo.EstadoCampanaFarmacia;
import com.farmamia.posupdate.dominio.modelo.EstadoCampanaGrupoTrx;
import com.farmamia.posupdate.dominio.modelo.EstadoOperacionalCampanaFarmacia;
import com.farmamia.posupdate.dominio.modelo.EstadoTecnicoCampanaFarmacia;
import com.farmamia.posupdate.dominio.modelo.ResumenCampanaGruposTrx;
import com.farmamia.posupdate.dominio.puerto.RepositorioCampanaGruposTrx;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.AlertaEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.CampanaGrupoTrxEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.DespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.EquipoGrupoTrxEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.GrupoTrxEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.SucursalEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.AlertaRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.CampanaGrupoTrxRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.DespliegueRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.EquipoGrupoTrxRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.GrupoTrxRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RepositorioCampanaGruposTrxJpaAdaptador implements RepositorioCampanaGruposTrx {

    private static final List<String> ESTADOS_ALERTA_ABIERTA = List.of("OPEN", "ACKNOWLEDGED");
    private static final List<String> ESTADOS_FINALES = List.of("COMPLETED", "FAILED", "ROLLBACK_COMPLETED", "ROLLBACK_FAILED", "SKIPPED");
    private static final List<String> ESTADOS_SIN_INICIO = List.of("PENDING", "AUTHORIZED", "WAITING_WINDOW", "WAITING_ACTIVITY", "OFFLINE");

    private final DespliegueRepositorioJpa despliegueRepositorioJpa;
    private final GrupoTrxRepositorioJpa grupoTrxRepositorioJpa;
    private final EquipoGrupoTrxRepositorioJpa equipoGrupoTrxRepositorioJpa;
    private final ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa;
    private final CampanaGrupoTrxRepositorioJpa campanaGrupoTrxRepositorioJpa;
    private final AlertaRepositorioJpa alertaRepositorioJpa;

    public RepositorioCampanaGruposTrxJpaAdaptador(
        DespliegueRepositorioJpa despliegueRepositorioJpa,
        GrupoTrxRepositorioJpa grupoTrxRepositorioJpa,
        EquipoGrupoTrxRepositorioJpa equipoGrupoTrxRepositorioJpa,
        ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa,
        CampanaGrupoTrxRepositorioJpa campanaGrupoTrxRepositorioJpa,
        AlertaRepositorioJpa alertaRepositorioJpa
    ) {
        this.despliegueRepositorioJpa = despliegueRepositorioJpa;
        this.grupoTrxRepositorioJpa = grupoTrxRepositorioJpa;
        this.equipoGrupoTrxRepositorioJpa = equipoGrupoTrxRepositorioJpa;
        this.objetivoDespliegueRepositorioJpa = objetivoDespliegueRepositorioJpa;
        this.campanaGrupoTrxRepositorioJpa = campanaGrupoTrxRepositorioJpa;
        this.alertaRepositorioJpa = alertaRepositorioJpa;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenCampanaGruposTrx estadoPorTrx(UUID idCampana) {
        DespliegueEntidad campana = buscarCampana(idCampana);
        List<CampanaGrupoTrx> grupos = gruposCampana(campana).stream()
            .map(grupo -> calcular(campana, grupo, true))
            .toList();

        Set<UUID> farmaciasAfectadas = new HashSet<>();
        Set<UUID> farmaciasTurno = new HashSet<>();
        Set<UUID> farmaciasCriticas = new HashSet<>();
        for (CampanaGrupoTrx grupo : grupos) {
            for (EstadoCampanaFarmacia farmacia : grupo.farmacias()) {
                farmaciasAfectadas.add(farmacia.farmaciaId());
                if (farmacia.deTurno() && farmacia.estadoOperacional() != EstadoOperacionalCampanaFarmacia.NORMAL) {
                    farmaciasTurno.add(farmacia.farmaciaId());
                }
                if (farmacia.estadoOperacional() == EstadoOperacionalCampanaFarmacia.CRITICA) {
                    farmaciasCriticas.add(farmacia.farmaciaId());
                }
            }
        }

        return new ResumenCampanaGruposTrx(
            campana.getId(),
            campana.getNombre(),
            campana.getPaquete().getVersion(),
            campana.getEstado(),
            grupos.size(),
            (int) grupos.stream().filter(grupo -> grupo.estado() == EstadoCampanaGrupoTrx.EN_RIESGO || grupo.estado() == EstadoCampanaGrupoTrx.FALLIDO).count(),
            (int) grupos.stream().filter(grupo -> grupo.estado() == EstadoCampanaGrupoTrx.PAUSADO).count(),
            farmaciasAfectadas.size(),
            farmaciasTurno.size(),
            farmaciasCriticas.size(),
            grupos
        );
    }

    @Override
    @Transactional
    public CampanaGrupoTrx asociar(UUID idCampana, UUID idGrupoTrx) {
        DespliegueEntidad campana = buscarCampana(idCampana);
        GrupoTrxEntidad grupo = buscarGrupo(idGrupoTrx);
        if (!"ACTIVO".equals(grupo.getEstado()) || !grupo.isActivo()) {
            throw new IllegalArgumentException("Solo se pueden asociar Grupos TRX activos a una campana POS.");
        }
        if (campanaGrupoTrxRepositorioJpa.existsByCampana_IdAndGrupoTrx_Id(idCampana, idGrupoTrx)) {
            return calcular(campana, campanaGrupoTrxRepositorioJpa.findByCampana_IdAndGrupoTrx_Id(idCampana, idGrupoTrx).orElseThrow(), true);
        }
        int orden = (int) campanaGrupoTrxRepositorioJpa.countByCampana_Id(idCampana) + 1;
        CampanaGrupoTrxEntidad entidad = campanaGrupoTrxRepositorioJpa.save(new CampanaGrupoTrxEntidad(campana, grupo, orden));
        vincularObjetivos(campana, grupo);
        CampanaGrupoTrx calculado = calcular(campana, entidad, true);
        entidad.actualizarEstadoCalculado(calculado.estado().name());
        return calcular(campana, campanaGrupoTrxRepositorioJpa.save(entidad), true);
    }

    @Override
    @Transactional
    public void quitar(UUID idCampana, UUID idGrupoTrx) {
        CampanaGrupoTrxEntidad entidad = buscarRelacion(idCampana, idGrupoTrx);
        List<ObjetivoDespliegueEntidad> objetivos = objetivosDeGrupo(buscarCampana(idCampana), entidad.getGrupoTrx());
        objetivos.forEach(objetivo -> objetivo.asignarGrupoTrx(null));
        objetivoDespliegueRepositorioJpa.saveAll(objetivos);
        campanaGrupoTrxRepositorioJpa.delete(entidad);
    }

    @Override
    @Transactional
    public CampanaGrupoTrx pausar(UUID idCampana, UUID idGrupoTrx, String motivo) {
        CampanaGrupoTrxEntidad entidad = buscarRelacion(idCampana, idGrupoTrx);
        entidad.pausar(motivo);
        return calcular(entidad.getCampana(), campanaGrupoTrxRepositorioJpa.save(entidad), true);
    }

    @Override
    @Transactional
    public CampanaGrupoTrx reanudar(UUID idCampana, UUID idGrupoTrx) {
        CampanaGrupoTrxEntidad entidad = buscarRelacion(idCampana, idGrupoTrx);
        entidad.reanudar();
        CampanaGrupoTrx calculado = calcular(entidad.getCampana(), entidad, true);
        entidad.actualizarEstadoCalculado(calculado.estado().name());
        return calcular(entidad.getCampana(), campanaGrupoTrxRepositorioJpa.save(entidad), true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean instruccionBloqueada(UUID idCampana, UUID idGrupoTrx, String codigoGrupoLegacy) {
        if (idGrupoTrx != null) {
            return campanaGrupoTrxRepositorioJpa.findByCampana_IdAndGrupoTrx_Id(idCampana, idGrupoTrx)
                .map(relacion -> "PAUSADO".equals(relacion.getEstado()) || "BLOQUEADO".equals(relacion.getEstado()) || "FALLIDO".equals(relacion.getEstado()))
                .orElse(false);
        }
        if (codigoGrupoLegacy == null || codigoGrupoLegacy.isBlank()) {
            return false;
        }
        return grupoTrxRepositorioJpa.findByCodigo(codigoGrupoLegacy.trim().toLowerCase(Locale.ROOT))
            .flatMap(grupo -> campanaGrupoTrxRepositorioJpa.findByCampana_IdAndGrupoTrx_Id(idCampana, grupo.getId()))
            .map(relacion -> "PAUSADO".equals(relacion.getEstado()) || "BLOQUEADO".equals(relacion.getEstado()) || "FALLIDO".equals(relacion.getEstado()))
            .orElse(false);
    }

    private List<CampanaGrupoTrxEntidad> gruposCampana(DespliegueEntidad campana) {
        List<CampanaGrupoTrxEntidad> formales = campanaGrupoTrxRepositorioJpa.findByCampana_IdOrderByOrdenAsc(campana.getId());
        if (!formales.isEmpty()) {
            return formales;
        }
        Map<String, GrupoTrxEntidad> gruposPorCodigo = grupoTrxRepositorioJpa.findAll().stream()
            .collect(Collectors.toMap(grupo -> grupo.getCodigo().toLowerCase(Locale.ROOT), Function.identity()));
        return objetivoDespliegueRepositorioJpa.findByDespliegue_Id(campana.getId()).stream()
            .map(ObjetivoDespliegueEntidad::getGrupoObjetivo)
            .filter(Objects::nonNull)
            .map(valor -> valor.trim().toLowerCase(Locale.ROOT))
            .distinct()
            .sorted()
            .map(gruposPorCodigo::get)
            .filter(Objects::nonNull)
            .map(grupo -> new CampanaGrupoTrxEntidad(campana, grupo, 1))
            .toList();
    }

    private CampanaGrupoTrx calcular(DespliegueEntidad campana, CampanaGrupoTrxEntidad relacion, boolean incluirFarmacias) {
        List<ObjetivoDespliegueEntidad> objetivos = objetivosDeGrupo(campana, relacion.getGrupoTrx());
        Map<UUID, AlertasFarmacia> alertasPorFarmacia = alertasPorFarmacia();
        List<EstadoCampanaFarmacia> farmacias = agruparPorFarmacia(objetivos).values().stream()
            .map(grupo -> calcularFarmacia(campana, relacion.getGrupoTrx(), grupo, alertasPorFarmacia))
            .sorted(Comparator.comparingInt(this::prioridadFarmacia).thenComparing(EstadoCampanaFarmacia::codigoFarmacia))
            .toList();
        int equiposTotales = objetivos.size();
        int completados = contar(objetivos, "COMPLETED");
        int fallidos = (int) objetivos.stream().filter(objetivo -> "FAILED".equals(objetivo.getEstado()) || "ROLLBACK_FAILED".equals(objetivo.getEstado())).count();
        int rollbacks = (int) objetivos.stream().filter(objetivo -> esRollback(objetivo.getEstado())).count();
        int pendientes = (int) objetivos.stream().filter(objetivo -> !ESTADOS_FINALES.contains(objetivo.getEstado())).count();
        int farmaciasCriticas = (int) farmacias.stream().filter(farmacia -> farmacia.estadoOperacional() == EstadoOperacionalCampanaFarmacia.CRITICA).count();
        int farmaciasTurno = (int) farmacias.stream().filter(farmacia -> farmacia.deTurno() && farmacia.estadoOperacional() != EstadoOperacionalCampanaFarmacia.NORMAL).count();
        int farmaciasPendientes = (int) farmacias.stream().filter(farmacia -> farmacia.pendientes() > 0).count();
        int farmaciasConFallos = (int) farmacias.stream().filter(farmacia -> farmacia.fallidos() > 0 || farmacia.rollbacks() > 0).count();
        EstadoCampanaGrupoTrx estado = estadoGrupo(relacion.getEstado(), objetivos, farmaciasCriticas, farmaciasTurno, fallidos, rollbacks, pendientes, completados);

        return new CampanaGrupoTrx(
            relacion.getId(),
            campana.getId(),
            campana.getNombre(),
            campana.getPaquete().getVersion(),
            campana.getEstado(),
            relacion.getGrupoTrx().getId(),
            relacion.getGrupoTrx().getCodigo(),
            relacion.getGrupoTrx().getNombre(),
            relacion.getOrden(),
            estado,
            farmacias.size(),
            farmaciasConFallos + farmaciasPendientes + farmaciasCriticas,
            farmaciasTurno,
            farmaciasCriticas,
            farmaciasPendientes,
            farmaciasConFallos,
            equiposTotales,
            completados,
            pendientes,
            fallidos,
            rollbacks,
            relacion.getMotivoPausa(),
            resumenRiesgo(farmaciasCriticas, farmaciasTurno, farmaciasConFallos, fallidos, rollbacks),
            relacion.getIniciadoEn(),
            relacion.getFinalizadoEn(),
            relacion.getCreadoEn(),
            relacion.getActualizadoEn(),
            incluirFarmacias ? farmacias : List.of()
        );
    }

    private void vincularObjetivos(DespliegueEntidad campana, GrupoTrxEntidad grupo) {
        Set<UUID> equiposGrupo = equipoGrupoTrxRepositorioJpa.findByGrupoTrxId(grupo.getId()).stream()
            .map(asignacion -> asignacion.getEquipo().getId())
            .collect(Collectors.toSet());
        List<ObjetivoDespliegueEntidad> objetivos = objetivoDespliegueRepositorioJpa.findByDespliegue_Id(campana.getId()).stream()
            .filter(objetivo -> equiposGrupo.contains(objetivo.getEquipo().getId())
                || grupo.getCodigo().equalsIgnoreCase(nuloAValor(objetivo.getGrupoObjetivo())))
            .toList();
        objetivos.forEach(objetivo -> objetivo.asignarGrupoTrx(grupo));
        objetivoDespliegueRepositorioJpa.saveAll(objetivos);
    }

    private List<ObjetivoDespliegueEntidad> objetivosDeGrupo(DespliegueEntidad campana, GrupoTrxEntidad grupo) {
        Set<UUID> equiposGrupo = equipoGrupoTrxRepositorioJpa.findByGrupoTrxId(grupo.getId()).stream()
            .map(asignacion -> asignacion.getEquipo().getId())
            .collect(Collectors.toSet());
        return objetivoDespliegueRepositorioJpa.findByDespliegue_Id(campana.getId()).stream()
            .filter(objetivo -> objetivo.getGrupoTrx() != null && objetivo.getGrupoTrx().getId().equals(grupo.getId())
                || objetivo.getGrupoTrx() == null && grupo.getCodigo().equalsIgnoreCase(nuloAValor(objetivo.getGrupoObjetivo()))
                || objetivo.getGrupoTrx() == null && equiposGrupo.contains(objetivo.getEquipo().getId()))
            .toList();
    }

    private Map<UUID, List<ObjetivoDespliegueEntidad>> agruparPorFarmacia(List<ObjetivoDespliegueEntidad> objetivos) {
        Map<UUID, List<ObjetivoDespliegueEntidad>> grupos = new LinkedHashMap<>();
        for (ObjetivoDespliegueEntidad objetivo : objetivos) {
            grupos.computeIfAbsent(objetivo.getEquipo().getSucursal().getId(), id -> new ArrayList<>()).add(objetivo);
        }
        return grupos;
    }

    private EstadoCampanaFarmacia calcularFarmacia(
        DespliegueEntidad campana,
        GrupoTrxEntidad grupoTrx,
        List<ObjetivoDespliegueEntidad> objetivos,
        Map<UUID, AlertasFarmacia> alertasPorFarmacia
    ) {
        SucursalEntidad farmacia = objetivos.get(0).getEquipo().getSucursal();
        AlertasFarmacia alertas = alertasPorFarmacia.getOrDefault(farmacia.getId(), new AlertasFarmacia(0, 0));
        int total = objetivos.size();
        int completados = contar(objetivos, "COMPLETED");
        int rollbacks = (int) objetivos.stream().filter(objetivo -> esRollback(objetivo.getEstado())).count();
        int fallidos = (int) objetivos.stream().filter(objetivo -> "FAILED".equals(objetivo.getEstado()) || "ROLLBACK_FAILED".equals(objetivo.getEstado())).count();
        int pendientes = (int) objetivos.stream().filter(objetivo -> !ESTADOS_FINALES.contains(objetivo.getEstado())).count();
        boolean todosSinInicio = objetivos.stream().allMatch(objetivo -> ESTADOS_SIN_INICIO.contains(objetivo.getEstado()));
        boolean hayOffline = objetivos.stream().anyMatch(objetivo -> "OFFLINE".equals(objetivo.getEstado()) || "OFFLINE".equals(objetivo.getEquipo().getEstado()));
        EstadoTecnicoCampanaFarmacia tecnico = estadoTecnico(total, completados, pendientes, fallidos, rollbacks, todosSinInicio);
        EstadoOperacionalCampanaFarmacia operacional = estadoOperacional(farmacia.isDeTurno(), tecnico, alertas, fallidos, rollbacks, hayOffline);
        return new EstadoCampanaFarmacia(
            farmacia.getId(),
            farmacia.getCodigo(),
            farmacia.getNombre(),
            campana.getId(),
            grupoTrx.getId(),
            grupoTrx.getCodigo(),
            farmacia.isDeTurno(),
            total,
            completados,
            pendientes,
            fallidos,
            rollbacks,
            ultimoHeartbeat(objetivos),
            alertas.criticas(),
            alertas.abiertas(),
            tecnico,
            operacional,
            resumenFarmacia(operacional, farmacia.isDeTurno(), alertas, fallidos, rollbacks, hayOffline),
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
        EstadoTecnicoCampanaFarmacia tecnico,
        AlertasFarmacia alertas,
        int fallidos,
        int rollbacks,
        boolean hayOffline
    ) {
        if (alertas.criticas() > 0 || tecnico == EstadoTecnicoCampanaFarmacia.FALLIDA || (deTurno && (hayOffline || fallidos > 0))) {
            return EstadoOperacionalCampanaFarmacia.CRITICA;
        }
        if (rollbacks > 0 || alertas.abiertas() > 0 || hayOffline || deTurno && tecnico != EstadoTecnicoCampanaFarmacia.COMPLETADA) {
            return EstadoOperacionalCampanaFarmacia.EN_RIESGO;
        }
        return EstadoOperacionalCampanaFarmacia.NORMAL;
    }

    private EstadoCampanaGrupoTrx estadoGrupo(
        String estadoPersistido,
        List<ObjetivoDespliegueEntidad> objetivos,
        int farmaciasCriticas,
        int farmaciasTurno,
        int fallidos,
        int rollbacks,
        int pendientes,
        int completados
    ) {
        if ("PAUSADO".equals(estadoPersistido)) {
            return EstadoCampanaGrupoTrx.PAUSADO;
        }
        if (fallidos > 0 && objetivos.stream().anyMatch(objetivo -> "ROLLBACK_FAILED".equals(objetivo.getEstado()))) {
            return EstadoCampanaGrupoTrx.FALLIDO;
        }
        if (farmaciasCriticas > 0) {
            return EstadoCampanaGrupoTrx.EN_RIESGO;
        }
        if (fallidos > 0) {
            return EstadoCampanaGrupoTrx.FALLIDO;
        }
        if (farmaciasTurno > 0 || rollbacks > 0) {
            return EstadoCampanaGrupoTrx.EN_RIESGO;
        }
        if (pendientes == 0 && rollbacks > 0) {
            return EstadoCampanaGrupoTrx.COMPLETADO_CON_FALLOS;
        }
        if (pendientes == 0 && completados == objetivos.size() && !objetivos.isEmpty()) {
            return EstadoCampanaGrupoTrx.COMPLETADO;
        }
        if (objetivos.isEmpty() || objetivos.stream().allMatch(objetivo -> ESTADOS_SIN_INICIO.contains(objetivo.getEstado()))) {
            return EstadoCampanaGrupoTrx.PENDIENTE;
        }
        return EstadoCampanaGrupoTrx.EN_EJECUCION;
    }

    private EquipoEstadoCampanaFarmacia aEquipo(ObjetivoDespliegueEntidad objetivo) {
        EquipoEntidad equipo = objetivo.getEquipo();
        return new EquipoEstadoCampanaFarmacia(
            equipo.getId(),
            equipo.getNombreEquipo(),
            equipo.getEstado(),
            objetivo.getEstado(),
            objetivo.getGrupoTrx() == null ? objetivo.getGrupoObjetivo() : objetivo.getGrupoTrx().getCodigo(),
            objetivo.getVersionAnterior(),
            objetivo.getVersionNueva(),
            objetivo.getUltimoError(),
            equipo.getUltimoLatidoEn(),
            esRollback(objetivo.getEstado())
        );
    }

    private Map<UUID, AlertasFarmacia> alertasPorFarmacia() {
        Map<UUID, AlertasFarmacia> resultado = new HashMap<>();
        for (AlertaEntidad alerta : alertaRepositorioJpa.findByEstadoIn(ESTADOS_ALERTA_ABIERTA)) {
            if (alerta.getEquipo() == null || alerta.getEquipo().getSucursal() == null) {
                continue;
            }
            UUID farmaciaId = alerta.getEquipo().getSucursal().getId();
            AlertasFarmacia actual = resultado.getOrDefault(farmaciaId, new AlertasFarmacia(0, 0));
            resultado.put(farmaciaId, new AlertasFarmacia(
                actual.abiertas() + 1,
                actual.criticas() + ("CRITICAL".equals(alerta.getSeveridad()) ? 1 : 0)
            ));
        }
        return resultado;
    }

    private int prioridadFarmacia(EstadoCampanaFarmacia estado) {
        if (estado.estadoOperacional() == EstadoOperacionalCampanaFarmacia.CRITICA) {
            return 0;
        }
        if (estado.deTurno() && estado.estadoOperacional() != EstadoOperacionalCampanaFarmacia.NORMAL) {
            return 1;
        }
        if (estado.estadoOperacional() == EstadoOperacionalCampanaFarmacia.EN_RIESGO) {
            return 2;
        }
        return 3;
    }

    private String resumenRiesgo(int farmaciasCriticas, int farmaciasTurno, int farmaciasConFallos, int fallidos, int rollbacks) {
        if (farmaciasCriticas > 0 || farmaciasTurno > 0) {
            return farmaciasCriticas + " farmacias criticas y " + farmaciasTurno + " farmacias de turno afectadas";
        }
        if (fallidos > 0 || farmaciasConFallos > 0) {
            return farmaciasConFallos + " farmacias con fallos y " + fallidos + " equipos POS fallidos";
        }
        if (rollbacks > 0) {
            return rollbacks + " rollbacks completados; requiere verificacion NOC";
        }
        return "Sin riesgo operacional relevante por farmacia";
    }

    private String resumenFarmacia(EstadoOperacionalCampanaFarmacia operacional, boolean deTurno, AlertasFarmacia alertas, int fallidos, int rollbacks, boolean hayOffline) {
        if (operacional == EstadoOperacionalCampanaFarmacia.CRITICA) {
            if (deTurno && hayOffline) {
                return "Farmacia de turno con POS offline";
            }
            if (alertas.criticas() > 0) {
                return "Alerta critica abierta durante campana POS";
            }
            return "Farmacia con fallo critico en campana POS";
        }
        if (operacional == EstadoOperacionalCampanaFarmacia.EN_RIESGO) {
            if (rollbacks > 0) {
                return "Rollback ejecutado en la farmacia";
            }
            if (fallidos > 0) {
                return "Farmacia con equipos POS fallidos";
            }
            if (deTurno) {
                return "Farmacia de turno pendiente en campana POS";
            }
        }
        return "Operacion sin riesgo relevante";
    }

    private DespliegueEntidad buscarCampana(UUID id) {
        return despliegueRepositorioJpa.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Campana POS no encontrada: " + id));
    }

    private GrupoTrxEntidad buscarGrupo(UUID id) {
        return grupoTrxRepositorioJpa.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Grupo TRX no encontrado: " + id));
    }

    private CampanaGrupoTrxEntidad buscarRelacion(UUID idCampana, UUID idGrupoTrx) {
        return campanaGrupoTrxRepositorioJpa.findByCampana_IdAndGrupoTrx_Id(idCampana, idGrupoTrx)
            .orElseThrow(() -> new RecursoNoEncontradoException("Grupo TRX no asociado a la campana POS."));
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

    private String nuloAValor(String valor) {
        return valor == null ? "" : valor;
    }

    private record AlertasFarmacia(int abiertas, int criticas) {
    }
}
