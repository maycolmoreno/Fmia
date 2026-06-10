package com.farmamia.operations.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FiltrosAlertasIntegracionTest extends BaseIntegracionApiTest {

    @Test
    void filtraAlertasPorEstadoSeveridadTipoEquipoSucursalFechaYPaginacion() throws Exception {
        RegistroAgente agente = registrarAgente("POS-IT-ALERT-FILTER", "FMA-IT-ALERT-FILTER", "2026.06.1");
        UUID objetivo = crearObjetivoConInstruccion(agente, "2026.06.2-it-alert-filter");

        mockMvc.perform(post("/api/agent/{deviceId}/update-result", agente.idEquipo())
                .header(HttpHeaders.AUTHORIZATION, bearerAgente(agente.tokenAgente()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "deploymentTargetId", objetivo.toString(),
                    "status", "FAILED",
                    "oldVersion", "2026.06.1",
                    "newVersion", "2026.06.2-it-alert-filter",
                    "message", "Fallo para filtros"
                ))))
            .andExpect(status().isAccepted());

        MvcResult resultado = mockMvc.perform(get("/api/alerts")
                .param("status", "OPEN")
                .param("severity", "CRITICAL")
                .param("type", "UPDATE_FAILED")
                .param("hostname", "ALERT-FILTER")
                .param("branchCode", "FMA-IT-ALERT-FILTER")
                .param("dateFrom", OffsetDateTime.now().minusDays(1).toString())
                .param("page", "0")
                .param("size", "10")
                .param("sort", "openedAt,desc")
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode alertas = json(resultado);
        assertTrue(alertas.size() >= 1);
        assertTrue(contieneAlerta(alertas, "POS-IT-ALERT-FILTER", "FMA-IT-ALERT-FILTER", "UPDATE_FAILED"));
    }

    private UUID crearObjetivoConInstruccion(RegistroAgente agente, String version) throws Exception {
        JsonNode paquete = aprobarPaquete(cargarPaquete(version, zipValido()).get("id").asText());
        mockMvc.perform(post("/api/deployments")
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "packageId", paquete.get("id").asText(),
                    "name", "Campana filtros alerta",
                    "targetGroup", "ALERTS",
                    "pilot", true,
                    "deviceIds", List.of(agente.idEquipo().toString())
                ))))
            .andExpect(status().isCreated());

        MvcResult instruccion = mockMvc.perform(get("/api/agent/{deviceId}/instructions", agente.idEquipo())
                .header(HttpHeaders.AUTHORIZATION, bearerAgente(agente.tokenAgente())))
            .andExpect(status().isOk())
            .andReturn();
        return UUID.fromString(json(instruccion).get("deploymentTargetId").asText());
    }

    private boolean contieneAlerta(JsonNode alertas, String hostname, String branchCode, String tipo) {
        for (JsonNode alerta : alertas) {
            if (hostname.equals(alerta.get("hostname").asText())
                && branchCode.equals(alerta.get("branchCode").asText())
                && tipo.equals(alerta.get("alertType").asText())) {
                return true;
            }
        }
        return false;
    }

}
