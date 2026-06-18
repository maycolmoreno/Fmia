package com.farmamia.posupdate.integracion;

import com.farmamia.posupdate.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.SucursalEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.SucursalRepositorioJpa;
import com.farmamia.posupdate.infraestructura.seguridad.FiltroAutenticacionAdministrativa;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EquiposAsignacionIntegracionTest extends BaseIntegracionApiTest {

    @Autowired
    private EquipoRepositorioJpa equipoRepositorioJpa;

    @Autowired
    private SucursalRepositorioJpa sucursalRepositorioJpa;

    // Mockeamos el filtro administrativo para evitar que intercepte y aborte la petición
    // por falta del token JWT real en la cabecera. De esta forma, Spring Security delega
    // el contexto de seguridad a la anotación @WithMockUser del test.
    @MockBean
    private FiltroAutenticacionAdministrativa filtroAutenticacionAdministrativa;

    @Test
    @WithMockUser(roles = "USER") // Rol no autorizado (se requiere OPERATOR o ADMIN)
    void asignarSucursalesRetorna403ParaUsuarioSinRolOperator() throws Exception {
        mockMvc.perform(post("/api/equipos-pos/asignacion-masiva")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "assignments", List.of(Map.of(
                        "deviceId", UUID.randomUUID().toString(),
                        "branchId", UUID.randomUUID().toString()
                    ))
                ))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void asignarSucursalesRetorna403ParaUsuarioAnonimo() throws Exception {
        // Al no tener @WithMockUser, el SecurityContext estará vacío
        mockMvc.perform(post("/api/equipos-pos/asignacion-masiva")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "assignments", List.of(Map.of(
                        "deviceId", UUID.randomUUID().toString(),
                        "branchId", UUID.randomUUID().toString()
                    ))
                ))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR") // Rol autorizado
    void asignarSucursalesRetornaOkYResumenConConteoParaOperator() throws Exception {
        // 1. Insertar sucursal y equipo huérfano de prueba en la base de datos
        SucursalEntidad sucursal = sucursalRepositorioJpa.save(new SucursalEntidad(
            "FMA-TEST-88", "Farmacia Prueba 88"
        ));

        EquipoEntidad equipo = new EquipoEntidad(null, "TEST-ORPHAN-88", "C:\\Farmamia\\POS");
        equipo.actualizarRegistro(null, "192.168.10.88", "00:11:22:33:44:88", "Windows 11", "1.0", "1.0", "C:\\Farmamia\\POS");
        equipoRepositorioJpa.save(equipo);

        try {
            // 2. Realizar petición POST con payload válido y verificar la respuesta exitosa
            mockMvc.perform(post("/api/equipos-pos/asignacion-masiva")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of(
                        "assignments", List.of(Map.of(
                            "deviceId", equipo.getId().toString(),
                            "branchId", sucursal.getId().toString()
                        ))
                    ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned").value(1))
                .andExpect(jsonPath("$.skipped").value(0));
                
        } finally {
            // 3. Limpiar los registros de prueba en la base de datos
            equipoRepositorioJpa.delete(equipo);
            sucursalRepositorioJpa.delete(sucursal);
        }
    }
}
