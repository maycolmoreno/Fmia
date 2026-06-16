package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.farmamia.operations.dominio.modelo.EstadoOperacionalFarmacia;
import com.farmamia.operations.dominio.puerto.RepositorioEstadoFarmacias;
import com.farmamia.operations.infraestructura.persistencia.entidad.AlertaEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.GrupoTrxEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.SucursalEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.AlertaRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.GrupoTrxRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.SucursalRepositorioJpa;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioEstadoFarmaciasJpaAdaptador implements RepositorioEstadoFarmacias {

    private static final List<String> ESTADOS_ALERTA_ABIERTA = List.of("OPEN", "ACKNOWLEDGED");
    private static final List<String> ESTADOS_CAMPANA_ACTIVA = List.of(
        "DRAFT", "SCHEDULED", "PILOT_RUNNING", "APPROVED", "RUNNING", "PAUSED"
    );
    private static final List<String> ESTADOS_OBJETIVO_PENDIENTE = List.of(
        "PENDING", "AUTHORIZED", "WAITING_WINDOW", "WAITING_ACTIVITY", "DOWNLOADING",
        "DOWNLOADED", "CHECKSUM_VALIDATED", "BACKING_UP", "CLOSING_POS", "UPDATING",
        "VALIDATING", "ROLLBACK_PENDING", "ROLLING_BACK", "OFFLINE"
    );
    private static final List<String> ESTADOS_OBJETIVO_FALLIDO = List.of("FAILED", "ROLLBACK_FAILED");
    private static final List<String> ESTADOS_ROLLBACK_COMPLETADO = List.of("ROLLBACK_COMPLETED");
    private static final long MINUTOS_HEARTBEAT_VENCIDO_TURNO = 15;

    private final SucursalRepositorioJpa sucursalRepositorioJpa;
    private final EquipoRepositorioJpa equipoRepositorioJpa;
    private final AlertaRepositorioJpa alertaRepositorioJpa;
    private final ObjetivoDespliegueRepositorioJpa objetivoRepositorioJpa;
    private final GrupoTrxRepositorioJpa grupoTrxRepositorioJpa;

    public RepositorioEstadoFarmaciasJpaAdaptador(
        SucursalRepositorioJpa sucursalRepositorioJpa,
        EquipoRepositorioJpa equipoRepositorioJpa,
        AlertaRepositorioJpa alertaRepositorioJpa,
        ObjetivoDespliegueRepositorioJpa objetivoRepositorioJpa,
        GrupoTrxRepositorioJpa grupoTrxRepositorioJpa
    ) {
        this.sucursalRepositorioJpa = sucursalRepositorioJpa;
        this.equipoRepositorioJpa = equipoRepositorioJpa;
        this.alertaRepositorioJpa = alertaRepositorioJpa;
        this.objetivoRepositorioJpa = objetivoRepositorioJpa;
        this.grupoTrxRepositorioJpa = grupoTrxRepositorioJpa;
    }

    @Override
    public List<EstadoOperacionalFarmacia> listar() {
        DatosAgregados datos = cargarDatosAgregados();
        return datos.farmacias().stream()
            .map(farmacia -> estado(farmacia, datos))
            .toList();
    }

    @Override
    public Optional<EstadoOperacionalFarmacia> buscarPorId(UUID idFarmacia) {
        DatosAgregados datos = cargarDatosAgregados();
        return datos.farmaciasPorId().values().stream()
            .filter(farmacia -> farmacia.getId().equals(idFarmacia))
            .findFirst()
            .map(farmacia -> estado(farmacia, datos));
    }

    private DatosAgregados cargarDatosAgregados() {
        List<SucursalEntidad> farmacias = sucursalRepositorioJpa.findAll();
        Map<UUID, SucursalEntidad> farmaciasPorId = farmacias.stream()
            .collect(Collectors.toMap(SucursalEntidad::getId, Function.identity()));
        Map<UUID, List<EquipoEntidad>> equiposPorFarmacia = equipoRepositorioJpa.findAll().stream()
            .collect(Collectors.groupingBy(equipo -> equipo.getSucursal().getId()));
        Map<UUID, List<AlertaEntidad>> alertasPorFarmacia = alertaRepositorioJpa.findByEstadoIn(ESTADOS_ALERTA_ABIERTA).stream()
            .filter(alerta -> (alerta.getEquipo() != null && alerta.getEquipo().getSucursal() != null)
                           || (alerta.getEquipo() == null && alerta.getSucursal() != null))
            .collect(Collectors.groupingBy(alerta -> alerta.getEquipo() != null
                ? alerta.getEquipo().getSucursal().getId()
                : alerta.getSucursal().getId()));
        Map<UUID, List<ObjetivoDespliegueEntidad>> objetivosPorFarmacia = objetivoRepositorioJpa.findAll().stream()
            .filter(objetivo -> objetivo.getEquipo() != null && objetivo.getEquipo().getSucursal() != null)
            .filter(objetivo -> objetivo.getDespliegue() != null)
            .filter(objetivo -> ESTADOS_CAMPANA_ACTIVA.contains(objetivo.getDespliegue().getEstado()))
            .collect(Collectors.groupingBy(objetivo -> objetivo.getEquipo().getSucursal().getId()));
        Map<String, GrupoTrxEntidad> gruposTrxPorCodigo = grupoTrxRepositorioJpa.findAll().stream()
            .collect(Collectors.toMap(grupo -> grupo.getCodigo().toLowerCase(Locale.ROOT), Function.identity()));
        return new DatosAgregados(farmacias, farmaciasPorId, equiposPorFarmacia, alertasPorFarmacia, objetivosPorFarmacia, gruposTrxPorCodigo);
    }

    private EstadoOperacionalFarmacia estado(SucursalEntidad farmacia, DatosAgregados datos) {
        List<EquipoEntidad> equipos = datos.equiposPorFarmacia().getOrDefault(farmacia.getId(), List.of());
        List<AlertaEntidad> alertas = datos.alertasPorFarmacia().getOrDefault(farmacia.getId(), List.of());
        List<ObjetivoDespliegueEntidad> objetivos = datos.objetivosPorFarmacia().getOrDefault(farmacia.getId(), List.of());

        int totalEquipos = equipos.size();
        int equiposOnline = (int) equipos.stream().filter(equipo -> "ONLINE".equals(equipo.getEstado())).count();
        int equiposOffline = (int) equipos.stream().filter(equipo -> !"ONLINE".equals(equipo.getEstado())).count();
        int equiposSinLatido = (int) equipos.stream().filter(equipo -> equipo.getUltimoLatidoEn() == null).count();
        OffsetDateTime ultimoLatido = equipos.stream()
            .map(EquipoEntidad::getUltimoLatidoEn)
            .filter(latido -> latido != null)
            .max(Comparator.naturalOrder())
            .orElse(null);
        boolean heartbeatVencido = ultimoLatido == null
            || ultimoLatido.isBefore(OffsetDateTime.now().minusMinutes(MINUTOS_HEARTBEAT_VENCIDO_TURNO));
        int alertasCriticas = (int) alertas.stream().filter(alerta -> "CRITICAL".equals(alerta.getSeveridad())).count();
        int objetivosPendientes = (int) objetivos.stream()
            .filter(objetivo -> ESTADOS_OBJETIVO_PENDIENTE.contains(objetivo.getEstado()))
            .count();
        int objetivosFallidos = (int) objetivos.stream()
            .filter(objetivo -> ESTADOS_OBJETIVO_FALLIDO.contains(objetivo.getEstado()))
            .count();
        int rollbacksCompletados = (int) objetivos.stream()
            .filter(objetivo -> ESTADOS_ROLLBACK_COMPLETADO.contains(objetivo.getEstado()))
            .count();
        boolean pendienteFueraVentana = objetivos.stream()
            .filter(objetivo -> ESTADOS_OBJETIVO_PENDIENTE.contains(objetivo.getEstado()))
            .anyMatch(this::fueraDeVentana);
        boolean grupoTrxPausadoConPendiente = objetivos.stream()
            .filter(objetivo -> ESTADOS_OBJETIVO_PENDIENTE.contains(objetivo.getEstado()))
            .map(ObjetivoDespliegueEntidad::getGrupoObjetivo)
            .filter(grupo -> grupo != null && !grupo.isBlank())
            .map(grupo -> datos.gruposTrxPorCodigo().get(grupo.toLowerCase(Locale.ROOT)))
            .anyMatch(grupo -> grupo != null && "PAUSADO".equals(grupo.getEstado()));
        int campanasActivas = (int) objetivos.stream()
            .map(objetivo -> objetivo.getDespliegue().getId())
            .distinct()
            .count();
        String campanaPrincipal = objetivos.stream()
            .map(objetivo -> objetivo.getDespliegue().getNombre())
            .filter(nombre -> nombre != null && !nombre.isBlank())
            .findFirst()
            .orElse(null);
        String grupoTrxPrincipal = grupoTrxPrincipal(objetivos);
        String versionDominante = versionDominante(equipos);
        boolean criticaBase = alertasCriticas > 0 || objetivosFallidos > 0;
        boolean criticaTurno = farmacia.isDeTurno()
            && (equiposOffline > 0 || heartbeatVencido || objetivosFallidos > 0 || pendienteFueraVentana);
        boolean critica = criticaBase || criticaTurno;
        boolean turnoEnRiesgo = farmacia.isDeTurno()
            && (critica || equiposOffline > 0 || objetivosPendientes > 0 || rollbacksCompletados > 0 || grupoTrxPausadoConPendiente);
        boolean riesgoTurno = farmacia.isDeTurno() && (rollbacksCompletados > 0 || grupoTrxPausadoConPendiente);
        String estado = estadoOperacional(critica, riesgoTurno, turnoEnRiesgo, alertas, equiposOffline, objetivosPendientes);

        return new EstadoOperacionalFarmacia(
            farmacia.getId(),
            farmacia.getCodigo(),
            farmacia.getNombre(),
            farmacia.getCiudad(),
            farmacia.getZona(),
            farmacia.isDeTurno(),
            farmacia.isActiva(),
            estado,
            critica,
            turnoEnRiesgo,
            totalEquipos,
            equiposOnline,
            equiposOffline,
            equiposSinLatido,
            ultimoLatido,
            alertas.size(),
            alertasCriticas,
            campanasActivas,
            objetivosPendientes,
            objetivosFallidos,
            campanaPrincipal,
            grupoTrxPrincipal,
            versionDominante,
            resumenRiesgo(
                critica,
                turnoEnRiesgo,
                farmacia.isDeTurno(),
                alertasCriticas,
                equiposOffline,
                heartbeatVencido,
                objetivosPendientes,
                objetivosFallidos,
                rollbacksCompletados,
                pendienteFueraVentana,
                grupoTrxPausadoConPendiente
            )
        );
    }

    private String estadoOperacional(
        boolean critica,
        boolean riesgoTurno,
        boolean turnoEnRiesgo,
        List<AlertaEntidad> alertas,
        int equiposOffline,
        int objetivosPendientes
    ) {
        if (critica) {
            return "CRITICA";
        }
        if (riesgoTurno || turnoEnRiesgo) {
            return "EN_RIESGO";
        }
        if (!alertas.isEmpty() || equiposOffline > 0 || objetivosPendientes > 0) {
            return "EN_RIESGO";
        }
        return "NORMAL";
    }

    private String resumenRiesgo(
        boolean critica,
        boolean turnoEnRiesgo,
        boolean deTurno,
        int alertasCriticas,
        int equiposOffline,
        boolean heartbeatVencido,
        int objetivosPendientes,
        int objetivosFallidos,
        int rollbacksCompletados,
        boolean pendienteFueraVentana,
        boolean grupoTrxPausadoConPendiente
    ) {
        if (deTurno && heartbeatVencido) {
            return "Farmacia de turno sin heartbeat reciente";
        }
        if (deTurno && equiposOffline > 0) {
            return "Farmacia de turno con POS offline";
        }
        if (deTurno && pendienteFueraVentana) {
            return "Farmacia de turno con campana pendiente fuera de ventana";
        }
        if (deTurno && objetivosFallidos > 0) {
            return "Farmacia de turno con fallo de actualizacion o rollback";
        }
        if (deTurno && rollbacksCompletados > 0) {
            return "Farmacia de turno con rollback completado; requiere verificacion";
        }
        if (deTurno && grupoTrxPausadoConPendiente) {
            return "Farmacia de turno pendiente en Grupo TRX pausado";
        }
        if (turnoEnRiesgo) {
            return "Farmacia de turno con riesgo operativo";
        }
        if (critica) {
            return "Farmacia critica: alertas=" + alertasCriticas + ", fallosCampana=" + objetivosFallidos;
        }
        if (equiposOffline > 0 || objetivosPendientes > 0) {
            return "Riesgo operativo: equiposOffline=" + equiposOffline + ", pendientesCampana=" + objetivosPendientes;
        }
        return "Operacion normal";
    }

    private boolean fueraDeVentana(ObjetivoDespliegueEntidad objetivo) {
        LocalTime ahora = LocalTime.now();
        LocalTime inicio = objetivo.getDespliegue().getHoraOficialActualizacion();
        LocalTime fin = objetivo.getDespliegue().getHoraForzadaActualizacion();
        if (inicio.equals(fin)) {
            return false;
        }
        boolean enVentana = inicio.isBefore(fin)
            ? !ahora.isBefore(inicio) && !ahora.isAfter(fin)
            : !ahora.isBefore(inicio) || !ahora.isAfter(fin);
        return !enVentana;
    }

    private String grupoTrxPrincipal(List<ObjetivoDespliegueEntidad> objetivos) {
        return objetivos.stream()
            .map(ObjetivoDespliegueEntidad::getGrupoObjetivo)
            .filter(grupo -> grupo != null && !grupo.isBlank())
            .findFirst()
            .orElse(null);
    }

    private String versionDominante(List<EquipoEntidad> equipos) {
        Map<String, Long> conteo = new HashMap<>();
        equipos.stream()
            .map(EquipoEntidad::getVersionPos)
            .filter(version -> version != null && !version.isBlank())
            .forEach(version -> conteo.merge(version, 1L, Long::sum));
        return conteo.entrySet().stream()
            .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    private record DatosAgregados(
        List<SucursalEntidad> farmacias,
        Map<UUID, SucursalEntidad> farmaciasPorId,
        Map<UUID, List<EquipoEntidad>> equiposPorFarmacia,
        Map<UUID, List<AlertaEntidad>> alertasPorFarmacia,
        Map<UUID, List<ObjetivoDespliegueEntidad>> objetivosPorFarmacia,
        Map<String, GrupoTrxEntidad> gruposTrxPorCodigo
    ) {
    }
}
