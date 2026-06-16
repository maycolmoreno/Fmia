package com.farmamia.operations.aplicacion.casouso;

import com.farmamia.operations.dominio.modelo.AlertaRed;
import com.farmamia.operations.dominio.modelo.Sucursal;
import com.farmamia.operations.dominio.puerto.RepositorioAlertas;
import com.farmamia.operations.dominio.puerto.RepositorioSucursales;
import com.farmamia.operations.presentacion.dto.PayloadWebhookAlertmanager;
import com.farmamia.operations.presentacion.dto.PayloadWebhookAlertmanager.AlertaWebhook;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcesarAlertaRedCasoUso {

    private static final Logger log = LoggerFactory.getLogger(ProcesarAlertaRedCasoUso.class);

    private static final Map<String, String> MAPA_TIPOS = Map.of(
        "EnlaceCaidoFarmacia",  "LINK_DOWN",
        "LatenciaAltaFarmacia", "LATENCIA_ALTA",
        "RouterReiniciado",     "ROUTER_REINICIADO",
        "VpnCaidaFarmacia",     "VPN_CAIDA"
    );

    private final RepositorioSucursales repositorioSucursales;
    private final RepositorioAlertas repositorioAlertas;

    public ProcesarAlertaRedCasoUso(
        RepositorioSucursales repositorioSucursales,
        RepositorioAlertas repositorioAlertas
    ) {
        this.repositorioSucursales = repositorioSucursales;
        this.repositorioAlertas = repositorioAlertas;
    }

    @Transactional
    public void procesar(PayloadWebhookAlertmanager payload) {
        List<AlertaWebhook> alertas = payload.alerts();
        if (alertas == null || alertas.isEmpty()) {
            return;
        }
        for (AlertaWebhook alerta : alertas) {
            procesarAlerta(alerta);
        }
    }

    private void procesarAlerta(AlertaWebhook alerta) {
        if (!"firing".equals(alerta.status())) {
            return;
        }

        Map<String, String> labels = alerta.labels();
        String branchCode = labels == null ? null : labels.get("branch_code");
        if (branchCode == null || branchCode.isBlank()) {
            log.debug("Alerta de red sin branch_code — ignorada. fingerprint={}", alerta.fingerprint());
            return;
        }

        Optional<Sucursal> sucursalOpt = repositorioSucursales.buscarPorCodigo(branchCode);
        if (sucursalOpt.isEmpty()) {
            log.warn("Alerta de red para farmacia desconocida branch_code={} — ignorada", branchCode);
            return;
        }

        Sucursal sucursal = sucursalOpt.get();
        String alertname = labels != null ? labels.getOrDefault("alertname", "NETWORK_EVENT") : "NETWORK_EVENT";
        String tipoAlerta = MAPA_TIPOS.getOrDefault(alertname, "NETWORK_EVENT");
        String severidad = determinarSeveridad(tipoAlerta, sucursal.deTurno());
        String titulo = construirTitulo(branchCode, sucursal, tipoAlerta);
        String mensaje = construirMensaje(sucursal, alerta);

        repositorioAlertas.guardarAlertaRed(new AlertaRed(branchCode, severidad, tipoAlerta, titulo, mensaje));
    }

    private String determinarSeveridad(String tipoAlerta, boolean deTurno) {
        if ("LINK_DOWN".equals(tipoAlerta)) {
            return deTurno ? "CRITICAL" : "HIGH";
        }
        return deTurno ? "HIGH" : "WARNING";
    }

    private String construirTitulo(String branchCode, Sucursal sucursal, String tipoAlerta) {
        String sufijo = sucursal.deTurno() ? " - DE TURNO" : "";
        return String.format("[%s%s] %s", branchCode, sufijo, descripcionTipo(tipoAlerta));
    }

    private String construirMensaje(Sucursal sucursal, AlertaWebhook alerta) {
        String resumen = alerta.annotations() != null
            ? alerta.annotations().getOrDefault("summary", "")
            : "";
        String sufijo = sucursal.deTurno() ? " (farmacia de turno)" : "";
        String base = String.format("Farmacia: %s%s.", sucursal.nombre(), sufijo);
        return resumen.isBlank() ? base : base + " " + resumen;
    }

    private String descripcionTipo(String tipoAlerta) {
        return switch (tipoAlerta) {
            case "LINK_DOWN"         -> "Enlace WAN caído";
            case "LATENCIA_ALTA"     -> "Latencia alta en enlace";
            case "ROUTER_REINICIADO" -> "Router reiniciado inesperadamente";
            case "VPN_CAIDA"         -> "Túnel VPN caído";
            default                  -> "Evento de red";
        };
    }
}
