package com.farmamia.posupdate.integracion;

import com.farmamia.posupdate.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.SucursalEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.SucursalRepositorioJpa;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EquiposSinSucursalIntegracionTest extends BaseIntegracionApiTest {

    @Autowired
    private EquipoRepositorioJpa equipoRepositorioJpa;

    @Autowired
    private SucursalRepositorioJpa sucursalRepositorioJpa;

    private String tokenOperator;
    private String tokenViewer;

    @BeforeEach
    void setupUsuarios() throws Exception {
        tokenOperator = login("operator", "operator123");
        tokenViewer = login("viewer", "viewer123");
    }

    @Test
    void listarEquiposSinSucursalExigeAutenticacionYFiltraOrdenadoPorHostname() throws Exception {
        // 1. Crear una sucursal de prueba para asociar a un equipo no huérfano
        SucursalEntidad sucursal = sucursalRepositorioJpa.save(new SucursalEntidad(
            "FMA-TEST-99", "Farmacia Prueba 99"
        ));

        // 2. Crear equipos de prueba (dos sin sucursal y uno con sucursal)
        // Usamos nombres y MACs únicos para evitar conflictos con datos de otros tests o semillas
        EquipoEntidad orphanZ = new EquipoEntidad(null, "TEST-ORPHAN-Z", "C:\\Farmamia\\POS");
        orphanZ.actualizarRegistro(null, "192.168.10.91", "00:11:22:33:44:91", "Windows 11", "1.0", "1.0", "C:\\Farmamia\\POS");
        
        EquipoEntidad orphanA = new EquipoEntidad(null, "TEST-ORPHAN-A", "C:\\Farmamia\\POS");
        orphanA.actualizarRegistro(null, "192.168.10.92", "00:11:22:33:44:92", "Windows 11", "1.0", "1.0", "C:\\Farmamia\\POS");
        
        EquipoEntidad assigned = new EquipoEntidad(sucursal, "TEST-ASSIGNED-X", "C:\\Farmamia\\POS");
        assigned.actualizarRegistro(sucursal, "192.168.10.93", "00:11:22:33:44:93", "Windows 11", "1.0", "1.0", "C:\\Farmamia\\POS");

        equipoRepositorioJpa.save(orphanZ);
        equipoRepositorioJpa.save(orphanA);
        equipoRepositorioJpa.save(assigned);

        try {
            // 3. Verificar que un usuario no autenticado (anónimo) recibe 401
            mockMvc.perform(get("/api/devices/sin-sucursal"))
                .andExpect(status().isUnauthorized());

            // 4. Verificar que un usuario con rol VIEWER recibe 403 (ya que exigirLectura requiere ADMIN, OPERATOR o AUDITOR)
            mockMvc.perform(get("/api/devices/sin-sucursal")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenViewer))
                .andExpect(status().isForbidden());

            // 5. Verificar que un usuario con rol OPERATOR puede realizar la consulta
            MvcResult resultado = mockMvc.perform(get("/api/devices/sin-sucursal")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOperator))
                .andExpect(status().isOk())
                .andReturn();

            JsonNode listNode = json(resultado);
            assertTrue(listNode.isArray());

            // 6. Filtrar para quedarnos únicamente con nuestros equipos de prueba y verificar el ordenamiento por hostname
            List<JsonNode> misHuerfanos = new ArrayList<>();
            boolean contieneAsignado = false;

            for (JsonNode node : listNode) {
                String name = node.get("nombreEquipo").asText();
                if ("TEST-ORPHAN-A".equals(name) || "TEST-ORPHAN-Z".equals(name)) {
                    misHuerfanos.add(node);
                } else if ("TEST-ASSIGNED-X".equals(name)) {
                    contieneAsignado = true;
                }
            }

            // Validar que se retornaron ambos equipos huérfanos de prueba
            assertEquals(2, misHuerfanos.size());
            // Validar que el equipo asignado no se incluye en la respuesta
            assertTrue(!contieneAsignado, "El equipo asignado no debería listarse");

            // Validar el orden alfabético ascendente: TEST-ORPHAN-A antes de TEST-ORPHAN-Z
            assertEquals("TEST-ORPHAN-A", misHuerfanos.get(0).get("nombreEquipo").asText());
            assertEquals("TEST-ORPHAN-Z", misHuerfanos.get(1).get("nombreEquipo").asText());

            // Validar que el ID de sucursal es null o vacío
            assertTrue(misHuerfanos.get(0).get("idSucursal").isNull() || misHuerfanos.get(0).get("idSucursal").asText().isEmpty());
            assertEquals("SIN ASIGNAR", misHuerfanos.get(0).get("nombreSucursal").asText());

        } finally {
            // Limpiar los equipos y la sucursal de prueba para no contaminar la base de datos
            equipoRepositorioJpa.delete(orphanZ);
            equipoRepositorioJpa.delete(orphanA);
            equipoRepositorioJpa.delete(assigned);
            sucursalRepositorioJpa.delete(sucursal);
        }
    }
}
