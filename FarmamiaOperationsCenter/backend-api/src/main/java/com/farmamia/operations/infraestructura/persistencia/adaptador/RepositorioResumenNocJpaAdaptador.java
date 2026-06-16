package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.farmamia.operations.dominio.modelo.ResumenNocDashboard.AlertaResumenNoc;
import com.farmamia.operations.dominio.modelo.ResumenNocDashboard.CampanaActivaNoc;
import com.farmamia.operations.dominio.modelo.ResumenNocDashboard.EstadoPosNoc;
import com.farmamia.operations.dominio.modelo.ResumenNocDashboard.EstadoRedNoc;
import com.farmamia.operations.dominio.puerto.RepositorioResumenNoc;
import com.farmamia.operations.infraestructura.persistencia.entidad.AlertaEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.DespliegueEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.SucursalEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.AlertaRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.DespliegueRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RepositorioResumenNocJpaAdaptador implements RepositorioResumenNoc {

    private static final List<String> ESTADOS_CAMPANA_ACTIVA = List.of("RUNNING", "PILOT_RUNNING");
    private static final List<String> ESTADOS_COMPLETADO = List.of("COMPLETED");
    private static final List<String> ESTADOS_FALLIDO = List.of("FAILED", "ROLLBACK_FAILED");

    private final AlertaRepositorioJpa alertaRepositorioJpa;
    private final EquipoRepositorioJpa equipoRepositorioJpa;
    private final DespliegueRepositorioJpa despliegueRepositorioJpa;
    private final ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa;

    public RepositorioResumenNocJpaAdaptador(
        AlertaRepositorioJpa alertaRepositorioJpa,
        EquipoRepositorioJpa equipoRepositorioJpa,
        DespliegueRepositorioJpa despliegueRepositorioJpa,
        ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa
    ) {
        this.alertaRepositorioJpa = alertaRepositorioJpa;
        this.equipoRepositorioJpa = equipoRepositorioJpa;
        this.despliegueRepositorioJpa = despliegueRepositorioJpa;
        this.objetivoDespliegueRepositorioJpa = objetivoDespliegueRepositorioJpa;
    }

    @Override
    @Transactional(readOnly = true)
    public EstadoRedNoc obtenerEstadoRed() {
        return new EstadoRedNoc(
            alertaRepositorioJpa.countByTipoAlertaAndEstado("LINK_DOWN", "OPEN"),
            alertaRepositorioJpa.countByTipoAlertaAndEstado("LATENCIA_ALTA", "OPEN"),
            alertaRepositorioJpa.countByTipoAlertaAndEstado("VPN_CAIDA", "OPEN")
        );
    }

    @Override
    @Transactional(readOnly = true)
    public EstadoPosNoc obtenerEstadoPos() {
        long totalPos = equipoRepositorioJpa.count();
        long posOnline = equipoRepositorioJpa.countByEstado("ONLINE");
        long posOffline = equipoRepositorioJpa.countByEstado("OFFLINE");
        long posEnRiesgo = Math.max(0L, totalPos - posOnline - posOffline);
        String versionActual = equipoRepositorioJpa
            .findVersionesPosPorFrecuencia(PageRequest.of(0, 1))
            .stream().findFirst().orElse(null);
        return new EstadoPosNoc(totalPos, posOnline, posOffline, posEnRiesgo, versionActual);
    }

    @Override
    @Transactional(readOnly = true)
    public CampanaActivaNoc obtenerCampanaActiva() {
        return despliegueRepositorioJpa
            .findFirstByEstadoInOrderByCreadoEnDesc(ESTADOS_CAMPANA_ACTIVA)
            .map(this::aCampanaActivaNoc)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaResumenNoc> obtenerAlertasRecientes(int limite) {
        return alertaRepositorioJpa
            .findByOrderByAbiertaEnDesc(PageRequest.of(0, limite))
            .stream()
            .map(this::aAlertaResumenNoc)
            .toList();
    }

    private CampanaActivaNoc aCampanaActivaNoc(DespliegueEntidad despliegue) {
        UUID idDespliegue = despliegue.getId();
        long total = objetivoDespliegueRepositorioJpa.countByDespliegue_Id(idDespliegue);
        long completados = objetivoDespliegueRepositorioJpa.countByDespliegue_IdAndEstadoIn(idDespliegue, ESTADOS_COMPLETADO);
        long fallidos = objetivoDespliegueRepositorioJpa.countByDespliegue_IdAndEstadoIn(idDespliegue, ESTADOS_FALLIDO);
        int progreso = total == 0 ? 0 : (int) ((completados * 100) / total);
        String versionPos = despliegue.getPaquete() != null ? despliegue.getPaquete().getVersion() : null;
        return new CampanaActivaNoc(idDespliegue, despliegue.getNombre(), versionPos, progreso, total, completados, fallidos);
    }

    private AlertaResumenNoc aAlertaResumenNoc(AlertaEntidad alerta) {
        EquipoEntidad equipo = alerta.getEquipo();
        SucursalEntidad sucursal = equipo != null ? equipo.getSucursal() : alerta.getSucursal();
        return new AlertaResumenNoc(
            alerta.getId(),
            sucursal != null ? sucursal.getId() : null,
            sucursal != null ? sucursal.getCodigo() : alerta.getCodigoSucursalRed(),
            alerta.getSeveridad(),
            alerta.getTipoAlerta(),
            alerta.getTitulo(),
            alerta.getEstado(),
            alerta.getAbiertaEn(),
            alerta.isEventoDeRed()
        );
    }
}
