package com.farmamia.posupdate.aplicacion.casouso;

import com.farmamia.posupdate.aplicacion.excepcion.ConflictoOperacionException;
import com.farmamia.posupdate.dominio.modelo.AsignacionEquipoSucursal;
import com.farmamia.posupdate.dominio.modelo.DatosAuditoria;
import com.farmamia.posupdate.dominio.modelo.Equipo;
import com.farmamia.posupdate.dominio.modelo.EquipoHuerfano;
import com.farmamia.posupdate.dominio.modelo.EstadoSugerenciaAprovisionamiento;
import com.farmamia.posupdate.dominio.modelo.ResumenAsignacionMasiva;
import com.farmamia.posupdate.dominio.modelo.SucursalSugerida;
import com.farmamia.posupdate.dominio.puerto.RepositorioAuditoria;
import com.farmamia.posupdate.dominio.puerto.RepositorioEquipos;
import com.farmamia.posupdate.dominio.puerto.RepositorioSucursales;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AprovisionarEquiposHuerfanosCasoUso {

    private static final Pattern PATRON_HOSTNAME_FARMACIA = Pattern.compile("^([A-Z0-9]{5})-.*$");

    private final RepositorioEquipos repositorioEquipos;
    private final RepositorioSucursales repositorioSucursales;
    private final RepositorioAuditoria repositorioAuditoria;

    public AprovisionarEquiposHuerfanosCasoUso(
        RepositorioEquipos repositorioEquipos,
        RepositorioSucursales repositorioSucursales,
        RepositorioAuditoria repositorioAuditoria
    ) {
        this.repositorioEquipos = repositorioEquipos;
        this.repositorioSucursales = repositorioSucursales;
        this.repositorioAuditoria = repositorioAuditoria;
    }

    public List<EquipoHuerfano> listarHuerfanos() {
        return repositorioEquipos.listarHuerfanos()
            .stream()
            .map(this::diagnosticar)
            .toList();
    }

    @Transactional
    public ResumenAsignacionMasiva asignarMasivamente(
        List<AsignacionEquipoSucursal> asignaciones,
        String usuarioOperador,
        String direccionIp
    ) {
        validarAsignaciones(asignaciones);

        Set<UUID> idsEquipos = asignaciones.stream()
            .map(AsignacionEquipoSucursal::idEquipo)
            .collect(Collectors.toSet());
        Set<UUID> idsSucursales = asignaciones.stream()
            .map(AsignacionEquipoSucursal::idSucursal)
            .collect(Collectors.toSet());

        long equiposHuerfanos = repositorioEquipos.contarHuerfanosPorIds(idsEquipos);
        if (equiposHuerfanos != idsEquipos.size()) {
            throw new ConflictoOperacionException("El lote contiene equipos inexistentes o ya asignados.");
        }

        long sucursalesExistentes = repositorioSucursales.contarPorIds(idsSucursales);
        if (sucursalesExistentes != idsSucursales.size()) {
            throw new IllegalArgumentException("El lote contiene farmacias inexistentes.");
        }

        repositorioEquipos.asignarSucursales(asignaciones);
        repositorioAuditoria.registrar(new DatosAuditoria(
            usuarioOperador,
            "APROVISIONAMIENTO_MASIVO_EQUIPOS",
            "devices",
            null,
            Map.of("estadoAnterior", "HUERFANO"),
            Map.of("assigned", asignaciones.size(), "skipped", 0),
            direccionIp
        ));

        return new ResumenAsignacionMasiva(asignaciones.size(), 0);
    }

    private EquipoHuerfano diagnosticar(Equipo equipo) {
        String hostname = equipo.nombreEquipo() == null ? "" : equipo.nombreEquipo().trim().toUpperCase(Locale.ROOT);
        Matcher matcher = PATRON_HOSTNAME_FARMACIA.matcher(hostname);
        if (!matcher.matches()) {
            return aHuerfano(equipo, EstadoSugerenciaAprovisionamiento.FORMATO_INVALIDO, null);
        }

        String codigoSucursal = matcher.group(1);
        return repositorioSucursales.buscarSugeridaPorCodigo(codigoSucursal)
            .map(sucursal -> aHuerfano(equipo, EstadoSugerenciaAprovisionamiento.SUGERENCIA_VALIDA, sucursal))
            .orElseGet(() -> aHuerfano(equipo, EstadoSugerenciaAprovisionamiento.FARMACIA_NO_EXISTE, null));
    }

    private EquipoHuerfano aHuerfano(
        Equipo equipo,
        EstadoSugerenciaAprovisionamiento estado,
        SucursalSugerida sucursal
    ) {
        return new EquipoHuerfano(
            equipo.id(),
            equipo.nombreEquipo(),
            equipo.direccionIp(),
            equipo.versionAgente(),
            equipo.versionPos(),
            equipo.registradoEn(),
            estado,
            sucursal == null ? null : sucursal.id(),
            sucursal == null ? null : sucursal.codigo(),
            sucursal == null ? null : sucursal.nombre(),
            sucursal == null ? null : sucursal.codigoGrupoTrx()
        );
    }

    private void validarAsignaciones(List<AsignacionEquipoSucursal> asignaciones) {
        if (asignaciones == null || asignaciones.isEmpty()) {
            throw new IllegalArgumentException("El lote de asignaciones no puede estar vacio.");
        }

        boolean contieneNulos = asignaciones.stream()
            .anyMatch(asignacion -> asignacion == null
                || asignacion.idEquipo() == null
                || asignacion.idSucursal() == null);
        if (contieneNulos) {
            throw new IllegalArgumentException("Cada asignacion debe incluir deviceId y branchId.");
        }

        int totalUnicos = asignaciones.stream()
            .map(AsignacionEquipoSucursal::idEquipo)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet())
            .size();
        if (totalUnicos != asignaciones.size()) {
            throw new IllegalArgumentException("El lote contiene equipos duplicados.");
        }
    }
}
