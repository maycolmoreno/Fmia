package com.farmamia.posupdate.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditoriaAdministrativaIntegracionTest extends BaseIntegracionApiTest {

    @Test
    void registraAuditoriaDeAccionesAdministrativasCriticas() throws Exception {
        RegistroAgente agente = registrarAgente("POS-IT-AUDIT-001", "FMA-IT-AUDIT", "2026.06.1");

        JsonNode paquete = cargarPaquete("2026.06.2-it-audit", zipValido());
        paquete = aprobarPaquete(paquete.get("id").asText());

        MvcResult despliegue = mockMvc.perform(post("/api/deployments")
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "packageId", paquete.get("id").asText(),
                    "name", "Campana auditoria IT",
                    "targetGroup", "AUDIT",
                    "pilot", true,
                    "deviceIds", List.of(agente.idEquipo().toString())
                ))))
            .andExpect(status().isCreated())
            .andReturn();

        String idDespliegue = json(despliegue).get("id").asText();
        mockMvc.perform(post("/api/deployments/{id}/pause", idDespliegue)
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
            .andExpect(status().isOk());

        JsonNode usuario = crearUsuario("audit-user-it", "AuditUser12345X", "VIEWER");
        mockMvc.perform(post("/api/admin/users/{id}/change-role", usuario.get("id").asText())
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("role", "AUDITOR"))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/users/{id}/deactivate", usuario.get("id").asText())
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
            .andExpect(status().isOk());

        JsonNode auditoria = listarAuditoria();
        assertTrue(contieneAccion(auditoria, "ADMIN_LOGIN"));
        assertTrue(contieneAccion(auditoria, "PACKAGE_UPLOADED"));
        assertTrue(contieneAccion(auditoria, "PACKAGE_APPROVED"));
        assertTrue(contieneAccion(auditoria, "DEPLOYMENT_CREATED"));
        assertTrue(contieneAccion(auditoria, "DEPLOYMENT_PAUSED"));
        assertTrue(contieneAccion(auditoria, "ADMIN_USER_ROLE_CHANGED"));
        assertTrue(contieneAccion(auditoria, "ADMIN_USER_DEACTIVATED"));
    }

    private JsonNode crearUsuario(String usuario, String contrasena, String rol) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/admin/users")
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "username", usuario,
                    "password", contrasena,
                    "fullName", usuario,
                    "email", usuario + "@farmamia.local",
                    "role", rol
                ))))
            .andExpect(status().isCreated())
            .andReturn();
        return json(resultado);
    }

    private JsonNode listarAuditoria() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/audit-logs?limit=100")
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
            .andExpect(status().isOk())
            .andReturn();
        return json(resultado);
    }

    private boolean contieneAccion(JsonNode auditoria, String accion) {
        for (JsonNode item : auditoria) {
            if (accion.equals(item.get("action").asText())) {
                return true;
            }
        }
        return false;
    }
}
