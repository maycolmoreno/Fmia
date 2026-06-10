package com.farmamia.operations.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaquetesZipIntegracionTest extends BaseIntegracionApiTest {

    @Test
    void cargaCalculaSha256ApruebaYDescargaPaqueteZipReal() throws Exception {
        byte[] zip = zipValido();
        JsonNode paquete = cargarPaquete("2026.06.2-it-packages", zip);

        assertEquals("2026.06.2-it-packages", paquete.get("version").asText());
        assertEquals("VALIDATED", paquete.get("status").asText());
        assertEquals(sha256(zip), paquete.get("sha256Checksum").asText());

        String idPaquete = paquete.get("id").asText();
        mockMvc.perform(get("/api/packages/{id}/download", idPaquete)
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/packages/{id}/approve", idPaquete)
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
            .andExpect(status().isOk());

        MvcResult descarga = mockMvc.perform(get("/api/packages/{id}/download", idPaquete)
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
            .andExpect(status().isOk())
            .andReturn();
        assertArrayEquals(zip, descarga.getResponse().getContentAsByteArray());
    }

    private String sha256(byte[] contenido) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
    }
}
