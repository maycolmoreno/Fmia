package com.farmamia.posupdate.infraestructura.persistencia.adaptador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.farmamia.posupdate.dominio.modelo.DatosCrearDespliegue;
import com.farmamia.posupdate.dominio.modelo.Despliegue;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.DespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.PaquetePosEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.SucursalEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.DespliegueRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.GrupoTrxRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.PaquetePosRepositorioJpa;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

// Cubre las reglas de negocio de campanas agregadas en Fase 3 (tope mensual, exclusividad de
// campana activa, y version/actualizacion unica por farmacia). No usa Testcontainers: mockea los
// repositorios JPA, ya que este adaptador no tenia ninguna prueba (ver auditoria).
class RepositorioDesplieguesJpaAdaptadorTest {

    private final DespliegueRepositorioJpa despliegueRepositorioJpa = mock(DespliegueRepositorioJpa.class);
    private final ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa =
        mock(ObjetivoDespliegueRepositorioJpa.class);
    private final PaquetePosRepositorioJpa paquetePosRepositorioJpa = mock(PaquetePosRepositorioJpa.class);
    private final EquipoRepositorioJpa equipoRepositorioJpa = mock(EquipoRepositorioJpa.class);
    private final GrupoTrxRepositorioJpa grupoTrxRepositorioJpa = mock(GrupoTrxRepositorioJpa.class);

    private final RepositorioDesplieguesJpaAdaptador adaptador = new RepositorioDesplieguesJpaAdaptador(
        despliegueRepositorioJpa,
        objetivoDespliegueRepositorioJpa,
        paquetePosRepositorioJpa,
        equipoRepositorioJpa,
        grupoTrxRepositorioJpa
    );

    @Test
    void crearRechazaCuandoSeAlcanzoElTopeMensualDeCampanias() {
        prepararPaqueteAprobado();
        when(despliegueRepositorioJpa.countByCreadoEnGreaterThanEqualAndCreadoEnLessThan(any(), any()))
            .thenReturn(3L);

        DatosCrearDespliegue datos = datosCrearConEquipos(List.of(UUID.randomUUID()));

        assertThrows(IllegalArgumentException.class, () -> adaptador.crear(datos));
        verify(despliegueRepositorioJpa, never()).save(any());
    }

    @Test
    void crearPermiteCuandoNoSeHaAlcanzadoElTopeMensual() {
        UUID idEquipo = UUID.randomUUID();
        EquipoEntidad equipo = equipoSinSucursal(idEquipo);
        prepararPaqueteAprobado();
        when(despliegueRepositorioJpa.countByCreadoEnGreaterThanEqualAndCreadoEnLessThan(any(), any()))
            .thenReturn(2L);
        when(equipoRepositorioJpa.findAllById(anyList())).thenReturn(List.of(equipo));
        when(equipoRepositorioJpa.findById(idEquipo)).thenReturn(Optional.of(equipo));
        when(despliegueRepositorioJpa.save(any(DespliegueEntidad.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objetivoDespliegueRepositorioJpa.countByDespliegue_Id(any())).thenReturn(1L);

        Despliegue resultado = adaptador.crear(datosCrearConEquipos(List.of(idEquipo)));

        assertEquals(1L, resultado.cantidadObjetivos());
        verify(objetivoDespliegueRepositorioJpa).saveAll(anyList());
    }

    @Test
    void crearRechazaCuandoLaFarmaciaObjetivoYaTieneUnaActualizacionActiva() {
        UUID idEquipo = UUID.randomUUID();
        UUID idSucursal = UUID.randomUUID();
        EquipoEntidad equipo = equipoConSucursal(idEquipo, idSucursal);
        prepararPaqueteAprobado();
        when(despliegueRepositorioJpa.countByCreadoEnGreaterThanEqualAndCreadoEnLessThan(any(), any()))
            .thenReturn(0L);
        when(equipoRepositorioJpa.findAllById(anyList())).thenReturn(List.of(equipo));
        when(objetivoDespliegueRepositorioJpa.buscarSucursalesConObjetivoActivo(anyList(), anyList()))
            .thenReturn(List.of(idSucursal));

        DatosCrearDespliegue datos = datosCrearConEquipos(List.of(idEquipo));

        assertThrows(IllegalArgumentException.class, () -> adaptador.crear(datos));
        verify(despliegueRepositorioJpa, never()).save(any());
    }

    @Test
    void crearPermiteCuandoLaFarmaciaObjetivoNoTieneActualizacionActiva() {
        UUID idEquipo = UUID.randomUUID();
        UUID idSucursal = UUID.randomUUID();
        EquipoEntidad equipo = equipoConSucursal(idEquipo, idSucursal);
        prepararPaqueteAprobado();
        when(despliegueRepositorioJpa.countByCreadoEnGreaterThanEqualAndCreadoEnLessThan(any(), any()))
            .thenReturn(0L);
        when(equipoRepositorioJpa.findAllById(anyList())).thenReturn(List.of(equipo));
        when(equipoRepositorioJpa.findById(idEquipo)).thenReturn(Optional.of(equipo));
        when(objetivoDespliegueRepositorioJpa.buscarSucursalesConObjetivoActivo(anyList(), anyList()))
            .thenReturn(List.of());
        when(despliegueRepositorioJpa.save(any(DespliegueEntidad.class))).thenAnswer(inv -> inv.getArgument(0));
        when(objetivoDespliegueRepositorioJpa.countByDespliegue_Id(any())).thenReturn(1L);

        Despliegue resultado = adaptador.crear(datosCrearConEquipos(List.of(idEquipo)));

        assertEquals(1L, resultado.cantidadObjetivos());
    }

    @Test
    void lanzarRechazaCuandoYaHayOtraCampaniaEnCurso() {
        UUID id = UUID.randomUUID();
        DespliegueEntidad despliegue = mock(DespliegueEntidad.class);
        when(despliegueRepositorioJpa.findById(id)).thenReturn(Optional.of(despliegue));
        when(despliegueRepositorioJpa.countByEstadoIn(List.of("PILOT_RUNNING", "RUNNING"))).thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> adaptador.lanzar(id));
        verify(despliegue, never()).lanzar();
    }

    @Test
    void lanzarPermiteCuandoNoHayOtraCampaniaEnCurso() {
        UUID id = UUID.randomUUID();
        DespliegueEntidad despliegue = mock(DespliegueEntidad.class);
        PaquetePosEntidad paquete = mock(PaquetePosEntidad.class);
        when(despliegue.getPaquete()).thenReturn(paquete);
        when(despliegueRepositorioJpa.findById(id)).thenReturn(Optional.of(despliegue));
        when(despliegueRepositorioJpa.countByEstadoIn(List.of("PILOT_RUNNING", "RUNNING"))).thenReturn(0L);
        when(objetivoDespliegueRepositorioJpa.countByDespliegue_Id(id)).thenReturn(5L);

        adaptador.lanzar(id);

        verify(despliegue).lanzar();
    }

    private void prepararPaqueteAprobado() {
        PaquetePosEntidad paquete = mock(PaquetePosEntidad.class);
        when(paquete.estaAprobado()).thenReturn(true);
        when(paquetePosRepositorioJpa.findById(any())).thenReturn(Optional.of(paquete));
    }

    private EquipoEntidad equipoSinSucursal(UUID id) {
        EquipoEntidad equipo = mock(EquipoEntidad.class);
        when(equipo.getId()).thenReturn(id);
        when(equipo.getSucursal()).thenReturn(null);
        return equipo;
    }

    private EquipoEntidad equipoConSucursal(UUID idEquipo, UUID idSucursal) {
        EquipoEntidad equipo = mock(EquipoEntidad.class);
        SucursalEntidad sucursal = mock(SucursalEntidad.class);
        when(sucursal.getId()).thenReturn(idSucursal);
        when(equipo.getId()).thenReturn(idEquipo);
        when(equipo.getSucursal()).thenReturn(sucursal);
        return equipo;
    }

    private DatosCrearDespliegue datosCrearConEquipos(List<UUID> idsEquipos) {
        return new DatosCrearDespliegue(
            UUID.randomUUID(),
            "Campana de prueba",
            "Descripcion",
            null,
            null,
            false,
            idsEquipos
        );
    }
}
