package com.farmamia.posupdate.aplicacion.casouso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.farmamia.posupdate.dominio.modelo.Equipo;
import com.farmamia.posupdate.dominio.puerto.RepositorioEquipos;
import com.farmamia.posupdate.dominio.puerto.RepositorioEventosActualizacion;
import com.farmamia.posupdate.dominio.puerto.RepositorioSucursales;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarCatalogoOperativoCasoUsoTest {

    @Test
    void listarEquiposSinSucursalRetornaEquiposHuerfanosDelRepositorio() {
        // 1. Mock de los puertos del caso de uso
        RepositorioEquipos repositorioEquipos = mock(RepositorioEquipos.class);
        RepositorioSucursales repositorioSucursales = mock(RepositorioSucursales.class);
        RepositorioEventosActualizacion repositorioEventosActualizacion = mock(RepositorioEventosActualizacion.class);

        // 2. Crear datos de prueba
        Equipo equipoHuerfano = new Equipo(
            UUID.randomUUID(),
            null,
            "",
            "SIN ASIGNAR",
            "TEST-ORPHAN-01",
            "192.168.1.50",
            "00:11:22:33:44:55",
            "Windows 10",
            "0.1.0",
            "2026.06.1",
            "C:\\Farmamia\\POS",
            "REGISTERED",
            null,
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );
        List<Equipo> mockHuerfanos = List.of(equipoHuerfano);

        // Configurar comportamiento del mock
        when(repositorioEquipos.listarHuerfanos()).thenReturn(mockHuerfanos);

        // 3. Instanciar caso de uso y ejecutar
        ConsultarCatalogoOperativoCasoUso casoUso = new ConsultarCatalogoOperativoCasoUso(
            repositorioEquipos,
            repositorioSucursales,
            repositorioEventosActualizacion
        );

        List<Equipo> resultado = casoUso.listarEquiposSinSucursal();

        // 4. Validaciones
        assertEquals(1, resultado.size());
        assertEquals("TEST-ORPHAN-01", resultado.get(0).nombreEquipo());
        verify(repositorioEquipos).listarHuerfanos();
    }
}
