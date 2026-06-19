package com.farmamia.posupdate.aplicacion.casouso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.farmamia.posupdate.dominio.modelo.AlertaEquipo;
import com.farmamia.posupdate.dominio.modelo.AlertaRed;
import com.farmamia.posupdate.dominio.modelo.AlertaRegistrada;
import com.farmamia.posupdate.dominio.modelo.CatalogoRegion;
import com.farmamia.posupdate.dominio.modelo.FiltroAlertas;
import com.farmamia.posupdate.dominio.modelo.FiltroSucursales;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.dominio.modelo.Sucursal;
import com.farmamia.posupdate.dominio.modelo.SucursalSugerida;
import com.farmamia.posupdate.dominio.puerto.RepositorioAlertas;
import com.farmamia.posupdate.dominio.puerto.RepositorioSucursales;
import com.farmamia.posupdate.presentacion.dto.PayloadWebhookAlertmanager;
import com.farmamia.posupdate.presentacion.dto.PayloadWebhookAlertmanager.AlertaWebhook;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcesarAlertaRedCasoUsoTest {

    @Test
    void alertaLinkDownEnFarmaciaDeTurnoGeneraCritical() {
        RepositorioSucursalesEnMemoria sucursales = new RepositorioSucursalesEnMemoria();
        sucursales.agregar(sucursal("FAR001", "Farmacia Norte", true));
        RepositorioAlertasEnMemoria alertas = new RepositorioAlertasEnMemoria();
        ProcesarAlertaRedCasoUso casoUso = new ProcesarAlertaRedCasoUso(sucursales, alertas);

        casoUso.procesar(payload("firing", "EnlaceCaidoFarmacia", "FAR001", "Enlace WAN caído"));

        assertEquals(1, alertas.alertasRed().size());
        AlertaRed guardada = alertas.alertasRed().get(0);
        assertEquals("CRITICAL", guardada.severidad());
        assertEquals("LINK_DOWN", guardada.tipoAlerta());
        assertEquals("FAR001", guardada.codigoSucursal());
        assertTrue(guardada.titulo().contains("DE TURNO"));
        assertTrue(guardada.mensaje().contains("(farmacia de turno)"));
    }

    @Test
    void alertaLinkDownEnFarmaciaNoTurnoGeneraHigh() {
        RepositorioSucursalesEnMemoria sucursales = new RepositorioSucursalesEnMemoria();
        sucursales.agregar(sucursal("FAR002", "Farmacia Sur", false));
        RepositorioAlertasEnMemoria alertas = new RepositorioAlertasEnMemoria();
        ProcesarAlertaRedCasoUso casoUso = new ProcesarAlertaRedCasoUso(sucursales, alertas);

        casoUso.procesar(payload("firing", "EnlaceCaidoFarmacia", "FAR002", "Enlace WAN caído"));

        assertEquals(1, alertas.alertasRed().size());
        assertEquals("HIGH", alertas.alertasRed().get(0).severidad());
    }

    @Test
    void alertaLatenciaEnFarmaciaDeTurnoGeneraHigh() {
        RepositorioSucursalesEnMemoria sucursales = new RepositorioSucursalesEnMemoria();
        sucursales.agregar(sucursal("FAR003", "Farmacia Este", true));
        RepositorioAlertasEnMemoria alertas = new RepositorioAlertasEnMemoria();
        ProcesarAlertaRedCasoUso casoUso = new ProcesarAlertaRedCasoUso(sucursales, alertas);

        casoUso.procesar(payload("firing", "LatenciaAltaFarmacia", "FAR003", "Latencia >150ms"));

        assertEquals(1, alertas.alertasRed().size());
        assertEquals("HIGH", alertas.alertasRed().get(0).severidad());
        assertEquals("LATENCIA_ALTA", alertas.alertasRed().get(0).tipoAlerta());
    }

    @Test
    void alertaSinBranchCodeEsIgnorada() {
        RepositorioSucursalesEnMemoria sucursales = new RepositorioSucursalesEnMemoria();
        RepositorioAlertasEnMemoria alertas = new RepositorioAlertasEnMemoria();
        ProcesarAlertaRedCasoUso casoUso = new ProcesarAlertaRedCasoUso(sucursales, alertas);

        PayloadWebhookAlertmanager sinBranchCode = new PayloadWebhookAlertmanager(
            "4", "key", 0, "firing", "receiver",
            Map.of(), Map.of(), Map.of(), "http://alertmanager",
            List.of(new AlertaWebhook("firing", Map.of("alertname", "EnlaceCaidoFarmacia"),
                Map.of(), OffsetDateTime.now(), OffsetDateTime.now(), "http://prometheus", "fp1"))
        );

        casoUso.procesar(sinBranchCode);

        assertTrue(alertas.alertasRed().isEmpty());
    }

    @Test
    void alertaResolvedEsIgnorada() {
        RepositorioSucursalesEnMemoria sucursales = new RepositorioSucursalesEnMemoria();
        sucursales.agregar(sucursal("FAR001", "Farmacia Norte", true));
        RepositorioAlertasEnMemoria alertas = new RepositorioAlertasEnMemoria();
        ProcesarAlertaRedCasoUso casoUso = new ProcesarAlertaRedCasoUso(sucursales, alertas);

        casoUso.procesar(payload("resolved", "EnlaceCaidoFarmacia", "FAR001", "Enlace restaurado"));

        assertTrue(alertas.alertasRed().isEmpty());
    }

    @Test
    void alertaConFarmaciaDesconocidaEsIgnorada() {
        RepositorioSucursalesEnMemoria sucursales = new RepositorioSucursalesEnMemoria();
        RepositorioAlertasEnMemoria alertas = new RepositorioAlertasEnMemoria();
        ProcesarAlertaRedCasoUso casoUso = new ProcesarAlertaRedCasoUso(sucursales, alertas);

        casoUso.procesar(payload("firing", "EnlaceCaidoFarmacia", "FAR999", "Enlace caído"));

        assertTrue(alertas.alertasRed().isEmpty());
    }

    // --- helpers ---

    private PayloadWebhookAlertmanager payload(String status, String alertname, String branchCode, String summary) {
        Map<String, String> labels = new HashMap<>();
        labels.put("alertname", alertname);
        labels.put("branch_code", branchCode);

        Map<String, String> annotations = new HashMap<>();
        annotations.put("summary", summary);

        AlertaWebhook alerta = new AlertaWebhook(
            status, labels, annotations,
            OffsetDateTime.now(), OffsetDateTime.parse("0001-01-01T00:00:00Z"),
            "http://prometheus/graph", "fp-" + branchCode
        );

        return new PayloadWebhookAlertmanager(
            "4", "groupKey", 0, status, "farmamia-backend",
            Map.of("alertname", alertname), labels, annotations,
            "http://alertmanager:9093", List.of(alerta)
        );
    }

    private Sucursal sucursal(String codigo, String nombre, boolean deTurno) {
        return new Sucursal(UUID.randomUUID(), codigo, nombre, null, null, null,
            deTurno, true, OffsetDateTime.now(), OffsetDateTime.now());
    }

    // --- implementaciones en memoria ---

    private static final class RepositorioSucursalesEnMemoria implements RepositorioSucursales {

        private final Map<String, Sucursal> porCodigo = new HashMap<>();

        void agregar(Sucursal sucursal) {
            porCodigo.put(sucursal.codigo(), sucursal);
        }

        @Override
        public Optional<Sucursal> buscarPorCodigo(String codigo) {
            return Optional.ofNullable(porCodigo.get(codigo));
        }

        @Override
        public Optional<SucursalSugerida> buscarSugeridaPorCodigo(String codigo) {
            Sucursal sucursal = porCodigo.get(codigo);
            return sucursal == null
                ? Optional.empty()
                : Optional.of(new SucursalSugerida(sucursal.id(), sucursal.codigo(), sucursal.nombre(), null));
        }

        @Override
        public long contarPorIds(Set<UUID> idsSucursales) {
            return idsSucursales.size();
        }

        @Override
        public UUID obtenerOCrearPorCodigo(String codigoSucursal) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Sucursal> listar() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<CatalogoRegion> listarCatalogoRegiones() {
            return List.of();
        }

        @Override
        public Pagina<Sucursal> listarPaginado(FiltroSucursales filtro) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RepositorioAlertasEnMemoria implements RepositorioAlertas {

        private final List<AlertaRed> alertasRed = new ArrayList<>();

        List<AlertaRed> alertasRed() {
            return alertasRed;
        }

        @Override
        public void guardarAlertaRed(AlertaRed alerta) {
            alertasRed.add(alerta);
        }

        @Override
        public void guardar(AlertaEquipo alerta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<AlertaRegistrada> listarRecientes(int limite) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<AlertaRegistrada> listarConFiltros(FiltroAlertas filtro) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Pagina<AlertaRegistrada> listarPaginado(FiltroAlertas filtro) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AlertaRegistrada reconocer(UUID idAlerta, String usuarioActor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AlertaRegistrada cerrar(UUID idAlerta, String usuarioActor) {
            throw new UnsupportedOperationException();
        }
    }
}
