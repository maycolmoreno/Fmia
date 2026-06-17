package com.farmamia.posupdate.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InstruccionesDespliegueIntegracionTest extends BaseIntegracionApiTest {

    @Test
    void despliegueAprobadoGeneraInstruccionUpdatePosParaAgente() throws Exception {
        RegistroAgente agente = registrarAgente("POS-IT-INST-001", "FMA-IT-INST", "2026.06.1");
        JsonNode paquete = cargarPaquete("2026.06.2-it-instructions", zipValido());
        paquete = aprobarPaquete(paquete.get("id").asText());

        mockMvc.perform(post("/api/deployments")
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "packageId", paquete.get("id").asText(),
                    "name", "Campana IT instrucciones",
                    "description", "Prueba de instrucciones",
                    "targetGroup", "IT",
                    "pilot", true,
                    "deviceIds", List.of(agente.idEquipo().toString())
                ))))
            .andExpect(status().isCreated());

        MvcResult resultado = mockMvc.perform(get("/api/agent/{deviceId}/instructions", agente.idEquipo())
                .header(HttpHeaders.AUTHORIZATION, bearerAgente(agente.tokenAgente())))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode instruccion = json(resultado);
        assertTrue(instruccion.get("hasInstruction").asBoolean());
        assertEquals("UPDATE_POS", instruccion.get("instructionType").asText());
        assertEquals(paquete.get("id").asText(), instruccion.get("packageId").asText());
        assertEquals(paquete.get("version").asText(), instruccion.get("version").asText());
        assertEquals(paquete.get("sha256Checksum").asText(), instruccion.get("sha256Checksum").asText());
        assertTrue(instruccion.get("downloadUrl").asText().contains("/api/packages/"));
    }
}
