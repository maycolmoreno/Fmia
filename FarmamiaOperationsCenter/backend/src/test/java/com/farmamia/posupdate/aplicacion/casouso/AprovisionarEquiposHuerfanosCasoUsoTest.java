package com.farmamia.posupdate.aplicacion.casouso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.farmamia.posupdate.aplicacion.excepcion.ConflictoOperacionException;
import com.farmamia.posupdate.dominio.modelo.AsignacionEquipoSucursal;
import com.farmamia.posupdate.dominio.modelo.AuditoriaRegistrada;
import com.farmamia.posupdate.dominio.modelo.CatalogoRegion;
import com.farmamia.posupdate.dominio.modelo.DatosAuditoria;
import com.farmamia.posupdate.dominio.modelo.DatosRegistroAgente;
import com.farmamia.posupdate.dominio.modelo.Equipo;
import com.farmamia.posupdate.dominio.modelo.EquipoHuerfano;
import com.farmamia.posupdate.dominio.modelo.EstadoSugerenciaAprovisionamiento;
import com.farmamia.posupdate.dominio.modelo.FiltroAuditoria;
import com.farmamia.posupdate.dominio.modelo.FiltroAuditoriaPaginada;
import com.farmamia.posupdate.dominio.modelo.FiltroEquipos;
import com.farmamia.posupdate.dominio.modelo.FiltroSucursales;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.dominio.modelo.ResumenAsignacionMasiva;
import com.farmamia.posupdate.dominio.modelo.Sucursal;
import com.farmamia.posupdate.dominio.modelo.SucursalSugerida;
import com.farmamia.posupdate.dominio.puerto.RepositorioAuditoria;
import com.farmamia.posupdate.dominio.puerto.RepositorioEquipos;
import com.farmamia.posupdate.dominio.puerto.RepositorioSucursales;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AprovisionarEquiposHuerfanosCasoUsoTest {

    @Test
    void listaHuerfanosConDiagnosticoDeSugerencia() {
        UUID idSucursal = UUID.randomUUID();
        RepositorioEquiposEnMemoria equipos = new RepositorioEquiposEnMemoria(List.of(
            equipo("ML001-ADM"),
            equipo("ML999-A"),
            equipo("DESKTOP-GENERICO")
        ));
        RepositorioSucursalesEnMemoria sucursales = new RepositorioSucursalesEnMemoria();
        sucursales.agregar(new SucursalSugerida(idSucursal, "ML001", "Farmacia ML001", "TRX001"));
        AprovisionarEquiposHuerfanosCasoUso casoUso = new AprovisionarEquiposHuerfanosCasoUso(
            equipos,
            sucursales,
            new RepositorioAuditoriaEnMemoria()
        );

        List<EquipoHuerfano> huerfanos = casoUso.listarHuerfanos();

        Map<String, EquipoHuerfano> porHostname = huerfanos.stream()
            .collect(Collectors.toMap(EquipoHuerfano::nombreEquipo, equipo -> equipo));
        assertEquals(EstadoSugerenciaAprovisionamiento.SUGERENCIA_VALIDA, porHostname.get("ML001-ADM").estadoSugerencia());
        assertEquals(idSucursal, porHostname.get("ML001-ADM").idSucursalSugerida());
        assertEquals("TRX001", porHostname.get("ML001-ADM").codigoGrupoTrxSugerido());
        assertEquals(EstadoSugerenciaAprovisionamiento.FARMACIA_NO_EXISTE, porHostname.get("ML999-A").estadoSugerencia());
        assertEquals(EstadoSugerenciaAprovisionamiento.FORMATO_INVALIDO, porHostname.get("DESKTOP-GENERICO").estadoSugerencia());
    }

    @Test
    void asignacionMasivaValidaTodoElLoteYRegistraAuditoria() {
        UUID idEquipo = UUID.randomUUID();
        UUID idSucursal = UUID.randomUUID();
        RepositorioEquiposEnMemoria equipos = new RepositorioEquiposEnMemoria(List.of(equipo(idEquipo, "ML001-A")));
        RepositorioSucursalesEnMemoria sucursales = new RepositorioSucursalesEnMemoria();
        sucursales.agregar(new SucursalSugerida(idSucursal, "ML001", "Farmacia ML001", "TRX001"));
        RepositorioAuditoriaEnMemoria auditoria = new RepositorioAuditoriaEnMemoria();
        AprovisionarEquiposHuerfanosCasoUso casoUso = new AprovisionarEquiposHuerfanosCasoUso(equipos, sucursales, auditoria);

        ResumenAsignacionMasiva resumen = casoUso.asignarMasivamente(
            List.of(new AsignacionEquipoSucursal(idEquipo, idSucursal)),
            "operador",
            "10.0.0.10"
        );

        assertEquals(1, resumen.asignados());
        assertEquals(0, resumen.omitidos());
        assertEquals(1, equipos.asignaciones.size());
        assertEquals("APROVISIONAMIENTO_MASIVO_EQUIPOS", auditoria.registros.getFirst().accion());
        assertEquals(1, auditoria.registros.getFirst().valoresNuevos().get("assigned"));
    }

    @Test
    void asignacionMasivaFallaSiUnEquipoNoSigueHuerfano() {
        UUID idEquipo = UUID.randomUUID();
        UUID idSucursal = UUID.randomUUID();
        RepositorioEquiposEnMemoria equipos = new RepositorioEquiposEnMemoria(List.of());
        RepositorioSucursalesEnMemoria sucursales = new RepositorioSucursalesEnMemoria();
        sucursales.agregar(new SucursalSugerida(idSucursal, "ML001", "Farmacia ML001", "TRX001"));
        AprovisionarEquiposHuerfanosCasoUso casoUso = new AprovisionarEquiposHuerfanosCasoUso(
            equipos,
            sucursales,
            new RepositorioAuditoriaEnMemoria()
        );

        assertThrows(ConflictoOperacionException.class, () -> casoUso.asignarMasivamente(
            List.of(new AsignacionEquipoSucursal(idEquipo, idSucursal)),
            "operador",
            "10.0.0.10"
        ));
        assertEquals(0, equipos.asignaciones.size());
    }

    private Equipo equipo(String hostname) {
        return equipo(UUID.randomUUID(), hostname);
    }

    private Equipo equipo(UUID idEquipo, String hostname) {
        return new Equipo(
            idEquipo,
            null,
            "",
            "SIN ASIGNAR",
            hostname,
            "10.0.0.5",
            null,
            "Windows 11",
            "0.1.0",
            "2026.06.1",
            "C:\\Farmamia\\POS",
            "REGISTERED",
            null,
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );
    }

    private static final class RepositorioEquiposEnMemoria implements RepositorioEquipos {
        private final Map<UUID, Equipo> huerfanos;
        private final List<AsignacionEquipoSucursal> asignaciones = new ArrayList<>();

        private RepositorioEquiposEnMemoria(List<Equipo> huerfanos) {
            this.huerfanos = huerfanos.stream().collect(Collectors.toMap(Equipo::id, equipo -> equipo));
        }

        @Override
        public Equipo registrarOActualizar(UUID idSucursal, DatosRegistroAgente datosRegistro) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Equipo> buscarPorId(UUID idEquipo) {
            return Optional.ofNullable(huerfanos.get(idEquipo));
        }

        @Override
        public List<Equipo> listar() {
            return List.copyOf(huerfanos.values());
        }

        @Override
        public Pagina<Equipo> listarPaginado(FiltroEquipos filtro) {
            return new Pagina<>(listar(), 0, 1, huerfanos.size(), 1, false);
        }

        @Override
        public List<Equipo> listarHuerfanos() {
            return listar();
        }

        @Override
        public long contarHuerfanosPorIds(Set<UUID> idsEquipos) {
            return idsEquipos.stream().filter(huerfanos::containsKey).count();
        }

        @Override
        public void asignarSucursales(List<AsignacionEquipoSucursal> asignaciones) {
            this.asignaciones.addAll(asignaciones);
        }

        @Override
        public void registrarLatido(UUID idEquipo, String versionPos) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void actualizarVersionPos(UUID idEquipo, String versionPos) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RepositorioSucursalesEnMemoria implements RepositorioSucursales {
        private final Map<String, SucursalSugerida> porCodigo = new HashMap<>();
        private final Map<UUID, SucursalSugerida> porId = new HashMap<>();

        void agregar(SucursalSugerida sucursal) {
            porCodigo.put(sucursal.codigo(), sucursal);
            porId.put(sucursal.id(), sucursal);
        }

        @Override
        public UUID obtenerOCrearPorCodigo(String codigoSucursal) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Sucursal> buscarPorCodigo(String codigo) {
            return Optional.empty();
        }

        @Override
        public Optional<SucursalSugerida> buscarSugeridaPorCodigo(String codigo) {
            return Optional.ofNullable(porCodigo.get(codigo));
        }

        @Override
        public long contarPorIds(Set<UUID> idsSucursales) {
            return idsSucursales.stream().filter(porId::containsKey).count();
        }

        @Override
        public List<Sucursal> listar() {
            return List.of();
        }

        @Override
        public List<CatalogoRegion> listarCatalogoRegiones() {
            return List.of();
        }

        @Override
        public Pagina<Sucursal> listarPaginado(FiltroSucursales filtro) {
            return new Pagina<>(List.of(), 0, 1, 0, 0, false);
        }
    }

    private static final class RepositorioAuditoriaEnMemoria implements RepositorioAuditoria {
        private final List<DatosAuditoria> registros = new ArrayList<>();

        @Override
        public void registrar(DatosAuditoria datos) {
            registros.add(datos);
        }

        @Override
        public List<AuditoriaRegistrada> listarRecientes(int limite) {
            return List.of();
        }

        @Override
        public List<AuditoriaRegistrada> listarConFiltros(FiltroAuditoria filtro) {
            return List.of();
        }

        @Override
        public Pagina<AuditoriaRegistrada> listarPaginado(FiltroAuditoriaPaginada filtro) {
            return new Pagina<>(List.of(), 0, 1, 0, 0, false);
        }
    }
}
