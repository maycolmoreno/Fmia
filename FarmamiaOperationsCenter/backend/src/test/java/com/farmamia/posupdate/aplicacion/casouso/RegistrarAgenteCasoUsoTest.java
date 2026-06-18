package com.farmamia.posupdate.aplicacion.casouso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.farmamia.posupdate.dominio.modelo.DatosRegistroAgente;
import com.farmamia.posupdate.dominio.modelo.AsignacionEquipoSucursal;
import com.farmamia.posupdate.dominio.modelo.Equipo;
import com.farmamia.posupdate.dominio.modelo.FiltroEquipos;
import com.farmamia.posupdate.dominio.modelo.FiltroSucursales;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.dominio.modelo.RegistroAgente;
import com.farmamia.posupdate.dominio.modelo.Sucursal;
import com.farmamia.posupdate.dominio.modelo.SucursalSugerida;
import com.farmamia.posupdate.dominio.puerto.HasherTokens;
import com.farmamia.posupdate.dominio.puerto.RepositorioEquipos;
import com.farmamia.posupdate.dominio.puerto.RepositorioSucursales;
import com.farmamia.posupdate.dominio.puerto.RepositorioTokensAgente;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegistrarAgenteCasoUsoTest {

    @Test
    void registraAgenteLegacyConSucursalInformada() {
        UUID idSucursal = UUID.randomUUID();
        RepositorioSucursalesEnMemoria sucursales = new RepositorioSucursalesEnMemoria(idSucursal);
        RepositorioEquiposEnMemoria equipos = new RepositorioEquiposEnMemoria();
        RepositorioTokensEnMemoria tokens = new RepositorioTokensEnMemoria();
        RegistrarAgenteCasoUso casoUso = new RegistrarAgenteCasoUso(sucursales, equipos, tokens, token -> "hash-" + token);

        RegistroAgente registro = casoUso.registrar(datosRegistro("ML001"));

        assertNotNull(registro.idEquipo());
        assertEquals("ML001", sucursales.codigoSolicitado);
        assertEquals(idSucursal, equipos.idSucursalRecibida);
        assertEquals(registro.idEquipo(), tokens.idEquipo);
        assertNotNull(tokens.hashToken);
    }

    @Test
    void registraAgenteHuerfanoCuandoCodigoSucursalVieneVacio() {
        RepositorioSucursalesEnMemoria sucursales = new RepositorioSucursalesEnMemoria(UUID.randomUUID());
        RepositorioEquiposEnMemoria equipos = new RepositorioEquiposEnMemoria();
        RepositorioTokensEnMemoria tokens = new RepositorioTokensEnMemoria();
        RegistrarAgenteCasoUso casoUso = new RegistrarAgenteCasoUso(sucursales, equipos, tokens, token -> "hash-" + token);

        RegistroAgente registro = casoUso.registrar(datosRegistro(" "));

        assertNotNull(registro.idEquipo());
        assertNull(sucursales.codigoSolicitado);
        assertNull(equipos.idSucursalRecibida);
        assertEquals(registro.idEquipo(), tokens.idEquipo);
    }

    private DatosRegistroAgente datosRegistro(String codigoSucursal) {
        return new DatosRegistroAgente(
            codigoSucursal,
            "ML001-ADM",
            "10.0.0.5",
            "00-11-22-33-44-55",
            "Windows 11",
            "0.1.0",
            "2026.06.1",
            "C:\\Farmamia\\POS"
        );
    }

    private static final class RepositorioSucursalesEnMemoria implements RepositorioSucursales {
        private final UUID idSucursal;
        private String codigoSolicitado;

        private RepositorioSucursalesEnMemoria(UUID idSucursal) {
            this.idSucursal = idSucursal;
        }

        @Override
        public UUID obtenerOCrearPorCodigo(String codigoSucursal) {
            this.codigoSolicitado = codigoSucursal;
            return idSucursal;
        }

        @Override
        public Optional<Sucursal> buscarPorCodigo(String codigo) {
            return Optional.empty();
        }

        @Override
        public Optional<SucursalSugerida> buscarSugeridaPorCodigo(String codigo) {
            return Optional.empty();
        }

        @Override
        public long contarPorIds(Set<UUID> idsSucursales) {
            return idsSucursales.size();
        }

        @Override
        public List<Sucursal> listar() {
            return List.of();
        }

        @Override
        public Pagina<Sucursal> listarPaginado(FiltroSucursales filtro) {
            return new Pagina<>(List.of(), 0, 1, 0, 0, false);
        }
    }

    private static final class RepositorioEquiposEnMemoria implements RepositorioEquipos {
        private UUID idSucursalRecibida;
        private UUID idEquipo;

        @Override
        public Equipo registrarOActualizar(UUID idSucursal, DatosRegistroAgente datosRegistro) {
            this.idSucursalRecibida = idSucursal;
            this.idEquipo = UUID.randomUUID();
            return new Equipo(
                idEquipo,
                idSucursal,
                idSucursal == null ? "" : datosRegistro.codigoSucursal(),
                idSucursal == null ? "SIN ASIGNAR" : "Farmacia demo",
                datosRegistro.nombreEquipo(),
                datosRegistro.direccionIp(),
                datosRegistro.direccionMac(),
                datosRegistro.versionWindows(),
                datosRegistro.versionAgente(),
                datosRegistro.versionPos(),
                datosRegistro.rutaPos(),
                "REGISTERED",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
            );
        }

        @Override
        public Optional<Equipo> buscarPorId(UUID idEquipo) {
            return Optional.empty();
        }

        @Override
        public List<Equipo> listar() {
            return List.of();
        }

        @Override
        public Pagina<Equipo> listarPaginado(FiltroEquipos filtro) {
            return new Pagina<>(List.of(), 0, 1, 0, 0, false);
        }

        @Override
        public List<Equipo> listarHuerfanos() {
            return List.of();
        }

        @Override
        public long contarHuerfanosPorIds(Set<UUID> idsEquipos) {
            return idsEquipos.size();
        }

        @Override
        public void asignarSucursales(List<AsignacionEquipoSucursal> asignaciones) {
        }

        @Override
        public void registrarLatido(UUID idEquipo, String versionPos) {
        }

        @Override
        public void actualizarVersionPos(UUID idEquipo, String versionPos) {
        }
    }

    private static final class RepositorioTokensEnMemoria implements RepositorioTokensAgente {
        private UUID idEquipo;
        private String hashToken;

        @Override
        public void renovarTokenActivo(UUID idEquipo, String hashToken) {
            this.idEquipo = idEquipo;
            this.hashToken = hashToken;
        }
    }
}
