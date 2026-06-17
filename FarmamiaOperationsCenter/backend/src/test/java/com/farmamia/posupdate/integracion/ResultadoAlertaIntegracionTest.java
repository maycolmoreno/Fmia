package com.farmamia.posupdate.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResultadoAlertaIntegracionTest extends BaseIntegracionApiTest {

    @Test
    void resultadoFallidoGeneraAlertaYResultadoCompletadoActualizaVersionSinNuevaAlerta() throws Exception {
        RegistroAgente agente = registrarAgente("POS-IT-RESULT-001", "FMA-IT-RESULT", "2026.06.1");

        FlujoDespliegue fallido = crearFlujo(agente, "2026.06.2-it-failed");
        mockMvc.perform(post("/api/agent/{deviceId}/events", agente.idEquipo())
                .header(HttpHeaders.AUTHORIZATION, bearerAgente(agente.tokenAgente()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "deploymentTargetId", fallido.idObjetivo().toString(),
                    "eventType", "UPDATE_FAILED",
                    "eventMessage", "Fallo controlado IT",
                    "oldVersion", "2026.06.1",
                    "newVersion", "2026.06.2-it-failed",
                    "metadata", Map.of("test", true)
                ))))
            .andExpect(status().isAccepted());

        reportarResultado(agente, fallido.idObjetivo(), "FAILED", "2026.06.1", "2026.06.2-it-failed", "Fallo controlado IT");

        JsonNode estadoFallido = estadoDespliegue(fallido.idDespliegue());
        assertEquals(1, estadoFallido.get("targetsByStatus").get("FAILED").asInt());

        JsonNode alertas = listarAlertas();
        long alertasCriticas = contarAlertas(alertas, "UPDATE_FAILED");
        assertTrue(alertasCriticas >= 1);

        FlujoDespliegue exitoso = crearFlujo(agente, "2026.06.2-it-success");
        reportarResultado(agente, exitoso.idObjetivo(), "COMPLETED", "2026.06.1", "2026.06.2-it-success", "Completado IT");

        JsonNode estadoExitoso = estadoDespliegue(exitoso.idDespliegue());
        assertEquals(1, estadoExitoso.get("targetsByStatus").get("COMPLETED").asInt());

        JsonNode equipo = obtenerEquipo(agente.idEquipo());
        assertEquals("2026.06.2-it-success", equipo.get("posVersion").asText());

        JsonNode alertasDespues = listarAlertas();
        assertEquals(alertasCriticas, contarAlertas(alertasDespues, "UPDATE_FAILED"));
    }

    private FlujoDespliegue crearFlujo(RegistroAgente agente, String version) throws Exception {
        JsonNode paquete = aprobarPaquete(cargarPaquete(version, zipValido()).get("id").asText());
        MvcResult despliegueResultado = mockMvc.perform(post("/api/deployments")
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "packageId", paquete.get("id").asText(),
                    "name", "Campana " + version,
                    "targetGroup", "IT",
                    "pilot", true,
                    "deviceIds", List.of(agente.idEquipo().toString())
                ))))
            .andExpect(status().isCreated())
            .andReturn();
        UUID idDespliegue = UUID.fromString(json(despliegueResultado).get("id").asText());

        MvcResult instruccionResultado = mockMvc.perform(get("/api/agent/{deviceId}/instructions", agente.idEquipo())
                .header(HttpHeaders.AUTHORIZATION, bearerAgente(agente.tokenAgente())))
            .andExpect(status().isOk())
            .andReturn();
        UUID idObjetivo = UUID.fromString(json(instruccionResultado).get("deploymentTargetId").asText());
        return new FlujoDespliegue(idDespliegue, idObjetivo);
    }

    private void reportarResultado(
        RegistroAgente agente,
        UUID idObjetivo,
        String estado,
        String versionAnterior,
        String versionNueva,
        String mensaje
    ) throws Exception {
        mockMvc.perform(post("/api/agent/{deviceId}/update-result", agente.idEquipo())
                .header(HttpHeaders.AUTHORIZATION, bearerAgente(agente.tokenAgente()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "deploymentTargetId", idObjetivo.toString(),
                    "status", estado,
                    "oldVersion", versionAnterior,
                    "newVersion", versionNueva,
                    "message", mensaje
                ))))
            .andExpect(status().isAccepted());
    }

    private JsonNode estadoDespliegue(UUID idDespliegue) throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/deployments/{id}/status", idDespliegue)
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
            .andExpect(status().isOk())
            .andReturn();
        return json(resultado);
    }

    private JsonNode obtenerEquipo(UUID idEquipo) throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/devices/{id}", idEquipo)
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
            .andExpect(status().isOk())
            .andReturn();
        return json(resultado).get("device");
    }

    private JsonNode listarAlertas() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/alerts?limit=100")
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
            .andExpect(status().isOk())
            .andReturn();
        return json(resultado);
    }

    private long contarAlertas(JsonNode alertas, String tipo) {
        long total = 0;
        for (JsonNode alerta : alertas) {
            if (tipo.equals(alerta.get("alertType").asText())) {
                total++;
            }
        }
        return total;
    }

    private record FlujoDespliegue(UUID idDespliegue, UUID idObjetivo) {
    }
}
