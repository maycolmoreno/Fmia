package com.farmamia.posupdate.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeguridadRolesIntegracionTest extends BaseIntegracionApiTest {

    @Test
    void permisosPorRolSeAplicanViaHttpYUsuarioInactivoNoIniciaSesion() throws Exception {
        JsonNode operator = crearUsuario("operator-it-roles", "Operator12345X", "OPERATOR");
        JsonNode auditor = crearUsuario("auditor-it-roles", "Auditor12345X", "AUDITOR");
        crearUsuario("viewer-it-roles", "Viewer12345X", "VIEWER");
        JsonNode inactive = crearUsuario("inactive-it-roles", "Inactive12345X", "VIEWER");

        String tokenOperator = login("operator-it-roles", "Operator12345X");
        String tokenAuditor = login("auditor-it-roles", "Auditor12345X");
        String tokenViewer = login("viewer-it-roles", "Viewer12345X");

        mockMvc.perform(get("/api/admin/users")
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOperator)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "username", "blocked-create-it",
                    "password", "Blocked12345X",
                    "fullName", "Blocked",
                    "email", "blocked-create-it@farmamia.local",
                    "role", "VIEWER"
                ))))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/audit-logs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAuditor))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/deployments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAuditor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of())))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/packages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenViewer))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/audit-logs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenViewer))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/users/{id}/deactivate", inactive.get("id").asText())
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", "inactive-it-roles", "password", "Inactive12345X"))))
            .andExpect(status().isUnauthorized());
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
}
