package com.farmamia.operations.aplicacion.casouso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.farmamia.operations.dominio.modelo.InstruccionAgente;
import com.farmamia.operations.dominio.puerto.RepositorioInstruccionesAgente;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarInstruccionesAgenteCasoUsoTest {

    @Test
    void devuelveInstruccionActualizacionCuandoRepositorioEncuentraTargetAutorizado() {
        UUID idEquipo = UUID.randomUUID();
        UUID idObjetivo = UUID.randomUUID();
        UUID idPaquete = UUID.randomUUID();
        InstruccionAgente instruccionEsperada = new InstruccionAgente(
            true,
            "UPDATE_POS",
            idObjetivo,
            idPaquete,
            "2026.06.2-success",
            "/api/packages/" + idPaquete + "/download",
            "abc123",
            null,
            null,
            null,
            null,
            LocalTime.of(23, 55),
            LocalTime.of(1, 0),
            List.of(LocalTime.of(0, 50), LocalTime.of(0, 55))
        );
        ConsultarInstruccionesAgenteCasoUso casoUso = new ConsultarInstruccionesAgenteCasoUso(
            new RepositorioInstruccionesFake(Optional.of(instruccionEsperada))
        );

        InstruccionAgente instruccion = casoUso.buscarSiguienteInstruccion(idEquipo);

        assertTrue(instruccion.tieneInstruccion());
        assertEquals("UPDATE_POS", instruccion.tipoInstruccion());
        assertEquals(idObjetivo, instruccion.idObjetivoDespliegue());
        assertEquals(idPaquete, instruccion.idPaquete());
        assertEquals("2026.06.2-success", instruccion.version());
        assertEquals("/api/packages/" + idPaquete + "/download", instruccion.urlDescarga());
        assertEquals("abc123", instruccion.checksumSha256());
    }

    @Test
    void devuelveInstruccionVaciaCuandoNoHayTargetDisponible() {
        UUID idEquipo = UUID.randomUUID();
        ConsultarInstruccionesAgenteCasoUso casoUso = new ConsultarInstruccionesAgenteCasoUso(
            new RepositorioInstruccionesFake(Optional.empty())
        );

        InstruccionAgente instruccion = casoUso.buscarSiguienteInstruccion(idEquipo);

        assertFalse(instruccion.tieneInstruccion());
        assertNull(instruccion.tipoInstruccion());
        assertNull(instruccion.idObjetivoDespliegue());
        assertNull(instruccion.idPaquete());
        assertNull(instruccion.version());
        assertNull(instruccion.urlDescarga());
        assertNull(instruccion.checksumSha256());
    }

    private record RepositorioInstruccionesFake(
        Optional<InstruccionAgente> instruccion
    ) implements RepositorioInstruccionesAgente {

        @Override
        public Optional<InstruccionAgente> buscarSiguienteParaEquipo(UUID idEquipo) {
            return instruccion;
        }
    }
}
