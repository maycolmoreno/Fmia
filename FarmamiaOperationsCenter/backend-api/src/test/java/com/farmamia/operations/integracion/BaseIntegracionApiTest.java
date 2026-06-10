package com.farmamia.operations.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
abstract class BaseIntegracionApiTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("farmamia_ops_test")
        .withUsername("farmamia")
        .withPassword("farmamia");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String tokenAdmin;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("farmamia.paquetes.ruta-almacenamiento", () -> "target/test-packages");
        registry.add("farmamia.security.jwt-secret", () -> "test-secret-integration-1234567890");
    }

    @BeforeEach
    void prepararSesionAdmin() throws Exception {
        tokenAdmin = login("admin", "admin123");
    }

    protected String login(String usuario, String contrasena) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", usuario, "password", contrasena))))
            .andExpect(status().isOk())
            .andReturn();
        return json(resultado).get("accessToken").asText();
    }

    protected RegistroAgente registrarAgente(String hostname, String branchCode, String posVersion) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/agent/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "branchCode", branchCode,
                    "hostname", hostname,
                    "ipAddress", "192.168.10.50",
                    "macAddress", "00-11-22-33-" + hostname.substring(Math.max(0, hostname.length() - 2)),
                    "windowsVersion", "Windows 11",
                    "agentVersion", "0.1.0-test",
                    "posVersion", posVersion,
                    "posPath", "C:\\Farmamia\\POS"
                ))))
            .andExpect(status().isCreated())
            .andReturn();
        JsonNode json = json(resultado);
        return new RegistroAgente(UUID.fromString(json.get("deviceId").asText()), json.get("agentToken").asText());
    }

    protected byte[] zipValido() throws Exception {
        return zip(Map.of(
            "Zabyca.Pos.Desktop.exe", "demo exe",
            "version.txt", "2026.06.2-success"
        ));
    }

    protected JsonNode cargarPaquete(String version, byte[] zip) throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
            "file",
            version + ".zip",
            "application/zip",
            zip
        );
        MvcResult resultado = mockMvc.perform(multipart("/api/packages")
                .file(archivo)
                .param("version", version)
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
            .andExpect(status().isOk())
            .andReturn();
        return json(resultado);
    }

    protected JsonNode aprobarPaquete(String idPaquete) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/packages/{id}/approve", idPaquete)
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin()))
            .andExpect(status().isOk())
            .andReturn();
        return json(resultado);
    }

    protected byte[] zip(Map<String, String> archivos) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> archivo : archivos.entrySet()) {
                zip.putNextEntry(new ZipEntry(archivo.getKey()));
                zip.write(archivo.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return salida.toByteArray();
    }

    protected String bearerAdmin() {
        return "Bearer " + tokenAdmin;
    }

    protected String bearerAgente(String token) {
        return "Bearer " + token;
    }

    protected String json(Object valor) throws Exception {
        return objectMapper.writeValueAsString(valor);
    }

    protected JsonNode json(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    protected HttpHeaders autorizacionAdmin() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, bearerAdmin());
        return headers;
    }

    protected record RegistroAgente(UUID idEquipo, String tokenAgente) {
    }
}
