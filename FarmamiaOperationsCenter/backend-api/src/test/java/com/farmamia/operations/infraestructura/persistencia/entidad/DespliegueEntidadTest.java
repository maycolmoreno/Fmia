package com.farmamia.operations.infraestructura.persistencia.entidad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class DespliegueEntidadTest {

    @Test
    void despliegueEnEjecucionPuedePausarseYReanudarse() {
        DespliegueEntidad despliegue = despliegueEnEjecucion();

        despliegue.pausar();
        assertEquals("PAUSED", despliegue.getEstado());

        despliegue.reanudar();
        assertEquals("RUNNING", despliegue.getEstado());
    }

    @Test
    void soloDesplieguePausadoPuedeReanudarse() {
        DespliegueEntidad despliegue = despliegueEnEjecucion();

        assertThrows(IllegalArgumentException.class, despliegue::reanudar);
    }

    @Test
    void despliegueFinalizadoNoPuedeCancelarse() {
        DespliegueEntidad despliegue = despliegueEnEjecucion();
        despliegue.cancelar();

        assertEquals("CANCELLED", despliegue.getEstado());
        assertThrows(IllegalArgumentException.class, despliegue::cancelar);
    }

    @Test
    void desplieguePausadoPuedeReprogramarse() {
        DespliegueEntidad despliegue = despliegueEnEjecucion();
        despliegue.pausar();

        OffsetDateTime programadoEn = OffsetDateTime.now().plusHours(2);
        despliegue.programar(programadoEn);

        assertEquals("SCHEDULED", despliegue.getEstado());
        assertEquals(programadoEn, despliegue.getProgramadoEn());
    }

    private DespliegueEntidad despliegueEnEjecucion() {
        PaquetePosEntidad paquete = new PaquetePosEntidad(
            "2026.06.test",
            "pos.zip",
            "target/test-packages/pos.zip",
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            1024L
        );
        paquete.aprobar();
        return new DespliegueEntidad(paquete, "Campana test", "Prueba de estados", null);
    }
}
