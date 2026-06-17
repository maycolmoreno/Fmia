package com.farmamia.posupdate.infraestructura.persistencia.adaptador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.farmamia.posupdate.dominio.puerto.RepositorioCampanaGruposTrx;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.DespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.PaquetePosEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RepositorioInstruccionesAgenteJpaAdaptadorTest {

    @Test
    void buscarSiguienteParaEquipoIncrementaMetricaCuandoEmiteInstruccion() {
        ObjetivoDespliegueRepositorioJpa repositorio = mock(ObjetivoDespliegueRepositorioJpa.class);
        SimpleMeterRegistry metricas = new SimpleMeterRegistry();
        RepositorioInstruccionesAgenteJpaAdaptador adaptador = new RepositorioInstruccionesAgenteJpaAdaptador(
            repositorio,
            repositorioCampanaGruposTrx(false),
            10,
            metricas
        );
        UUID idEquipo = UUID.randomUUID();
        ObjetivoDespliegueEntidad objetivo = objetivoEntregable();
        when(repositorio.findFirstByEquipo_IdAndEstadoOrderByActualizadoEnDesc(idEquipo, "AUTHORIZED"))
            .thenReturn(Optional.of(objetivo));
        when(repositorio.save(objetivo)).thenReturn(objetivo);

        assertTrue(adaptador.buscarSiguienteParaEquipo(idEquipo).isPresent());

        verify(objetivo).registrarLeaseInstruccion(org.mockito.ArgumentMatchers.any(OffsetDateTime.class), org.mockito.ArgumentMatchers.eq(10));
        assertEquals(1.0, metricas.counter("farmamia.orchestration.instructions.issued.total").count());
    }

    @Test
    void buscarSiguienteParaEquipoRegistraBloqueoPorLeaseActivo() {
        ObjetivoDespliegueRepositorioJpa repositorio = mock(ObjetivoDespliegueRepositorioJpa.class);
        SimpleMeterRegistry metricas = new SimpleMeterRegistry();
        RepositorioInstruccionesAgenteJpaAdaptador adaptador = new RepositorioInstruccionesAgenteJpaAdaptador(
            repositorio,
            repositorioCampanaGruposTrx(false),
            10,
            metricas
        );
        UUID idEquipo = UUID.randomUUID();
        ObjetivoDespliegueEntidad objetivo = objetivoEntregable();
        when(objetivo.tieneLeaseActivo(org.mockito.ArgumentMatchers.any(OffsetDateTime.class))).thenReturn(true);
        when(repositorio.findFirstByEquipo_IdAndEstadoOrderByActualizadoEnDesc(idEquipo, "AUTHORIZED"))
            .thenReturn(Optional.of(objetivo));

        assertTrue(adaptador.buscarSiguienteParaEquipo(idEquipo).isEmpty());

        verify(repositorio, never()).save(objetivo);
        assertEquals(
            1.0,
            metricas.counter("farmamia.orchestration.instructions.blocked.total", "reason", "active_lease").count()
        );
    }

    private ObjetivoDespliegueEntidad objetivoEntregable() {
        UUID idObjetivo = UUID.randomUUID();
        UUID idPaquete = UUID.randomUUID();
        ObjetivoDespliegueEntidad objetivo = mock(ObjetivoDespliegueEntidad.class);
        DespliegueEntidad despliegue = mock(DespliegueEntidad.class);
        PaquetePosEntidad paquete = mock(PaquetePosEntidad.class);

        when(objetivo.getId()).thenReturn(idObjetivo);
        when(objetivo.estaAutorizado()).thenReturn(true);
        when(objetivo.getDespliegue()).thenReturn(despliegue);
        when(objetivo.getOleada()).thenReturn(null);
        when(objetivo.tieneLeaseActivo(org.mockito.ArgumentMatchers.any(OffsetDateTime.class))).thenReturn(false);
        when(despliegue.getPaquete()).thenReturn(paquete);
        when(despliegue.puedeEntregarInstrucciones()).thenReturn(true);
        when(despliegue.getHoraOficialActualizacion()).thenReturn(LocalTime.of(23, 55));
        when(despliegue.getHoraForzadaActualizacion()).thenReturn(LocalTime.of(1, 0));
        when(paquete.estaAprobado()).thenReturn(true);
        when(paquete.getId()).thenReturn(idPaquete);
        when(paquete.getVersion()).thenReturn("2026.06.2");
        when(paquete.getChecksumSha256()).thenReturn("abc123");

        return objetivo;
    }

    private RepositorioCampanaGruposTrx repositorioCampanaGruposTrx(boolean bloquea) {
        RepositorioCampanaGruposTrx repositorio = mock(RepositorioCampanaGruposTrx.class);
        when(repositorio.instruccionBloqueada(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(bloquea);
        return repositorio;
    }
}
