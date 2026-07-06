package com.farmamia.posupdate.infraestructura.persistencia.adaptador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.farmamia.posupdate.dominio.modelo.AlertaEquipo;
import com.farmamia.posupdate.dominio.modelo.AlertaRed;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.AlertaEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.SucursalEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.AlertaRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.SucursalRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.UsuarioAppRepositorioJpa;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

// Cubre la deduplicacion (fix del bug real: Alertmanager reenvia alertas "firing" y antes cada
// entrega creaba una fila nueva) y la correlacion automatica por farmacia (alertas de red se
// enlazan a la primera activa = el "ancla" del incidente). Ver auditoria de alertas.
class RepositorioAlertasJpaAdaptadorTest {

    private final EquipoRepositorioJpa equipoRepositorioJpa = mock(EquipoRepositorioJpa.class);
    private final AlertaRepositorioJpa alertaRepositorioJpa = mock(AlertaRepositorioJpa.class);
    private final UsuarioAppRepositorioJpa usuarioAppRepositorioJpa = mock(UsuarioAppRepositorioJpa.class);
    private final SucursalRepositorioJpa sucursalRepositorioJpa = mock(SucursalRepositorioJpa.class);

    private final RepositorioAlertasJpaAdaptador adaptador = new RepositorioAlertasJpaAdaptador(
        equipoRepositorioJpa,
        alertaRepositorioJpa,
        usuarioAppRepositorioJpa,
        sucursalRepositorioJpa
    );

    @Test
    void guardarNoCreaAlertaDuplicadaSiYaHayUnaActivaParaElMismoEquipoYTipo() {
        UUID idEquipo = UUID.randomUUID();
        EquipoEntidad equipo = equipoSinSucursal(idEquipo);
        when(equipoRepositorioJpa.findById(idEquipo)).thenReturn(Optional.of(equipo));
        when(alertaRepositorioJpa.findByEquipo_IdAndTipoAlertaAndEstadoIn(eq(idEquipo), eq("NETWORK_LINK_DOWN"), anyList()))
            .thenReturn(List.of(mock(AlertaEntidad.class)));

        adaptador.guardar(new AlertaEquipo(idEquipo, "CRITICAL", "NETWORK_LINK_DOWN", "Enlace caido", "msg"));

        verify(alertaRepositorioJpa, never()).save(any());
    }

    @Test
    void guardarCreaAlertaYCorrelacionaConElAnclaDeLaFarmacia() {
        UUID idEquipo = UUID.randomUUID();
        UUID idSucursal = UUID.randomUUID();
        UUID idAncla = UUID.randomUUID();
        EquipoEntidad equipo = equipoConSucursal(idEquipo, idSucursal);
        when(equipoRepositorioJpa.findById(idEquipo)).thenReturn(Optional.of(equipo));
        when(alertaRepositorioJpa.findByEquipo_IdAndTipoAlertaAndEstadoIn(eq(idEquipo), eq("HIGH_CPU_USAGE"), anyList()))
            .thenReturn(List.of());

        AlertaEntidad ancla = mock(AlertaEntidad.class);
        when(ancla.getId()).thenReturn(idAncla);
        when(ancla.getCorrelacionId()).thenReturn(null);
        when(alertaRepositorioJpa.buscarPorSucursalTiposYEstados(eq(idSucursal), anyList(), anyList()))
            .thenReturn(List.of(ancla));

        adaptador.guardar(new AlertaEquipo(idEquipo, "CRITICAL", "HIGH_CPU_USAGE", "CPU alta", "msg"));

        ArgumentCaptor<AlertaEntidad> captor = ArgumentCaptor.forClass(AlertaEntidad.class);
        verify(alertaRepositorioJpa, times(1)).save(captor.capture());
        assertEquals(idAncla, captor.getValue().getCorrelacionId());
    }

    @Test
    void guardarNoCorrelacionaTiposQueNoSonDeRed() {
        UUID idEquipo = UUID.randomUUID();
        UUID idSucursal = UUID.randomUUID();
        EquipoEntidad equipo = equipoConSucursal(idEquipo, idSucursal);
        when(equipoRepositorioJpa.findById(idEquipo)).thenReturn(Optional.of(equipo));
        when(alertaRepositorioJpa.findByEquipo_IdAndTipoAlertaAndEstadoIn(eq(idEquipo), eq("UPDATE_FAILED"), anyList()))
            .thenReturn(List.of());

        adaptador.guardar(new AlertaEquipo(idEquipo, "CRITICAL", "UPDATE_FAILED", "Fallo actualizacion", "msg"));

        ArgumentCaptor<AlertaEntidad> captor = ArgumentCaptor.forClass(AlertaEntidad.class);
        verify(alertaRepositorioJpa, times(1)).save(captor.capture());
        assertNull(captor.getValue().getCorrelacionId());
        verify(alertaRepositorioJpa, never()).buscarPorSucursalTiposYEstados(any(), anyList(), anyList());
    }

    @Test
    void guardarAlertaRedNoCreaDuplicadaSiYaHayUnaActivaParaLaMismaFarmaciaYTipo() {
        UUID idSucursal = UUID.randomUUID();
        SucursalEntidad sucursal = sucursal(idSucursal, "FMA001");
        when(sucursalRepositorioJpa.findByCodigo("FMA001")).thenReturn(Optional.of(sucursal));
        when(alertaRepositorioJpa.findBySucursal_IdAndTipoAlertaAndEstadoIn(eq(idSucursal), eq("LINK_DOWN"), anyList()))
            .thenReturn(List.of(mock(AlertaEntidad.class)));

        adaptador.guardarAlertaRed(new AlertaRed("FMA001", "CRITICAL", "LINK_DOWN", "Enlace caido", "msg"));

        verify(alertaRepositorioJpa, never()).save(any());
    }

    @Test
    void guardarAlertaRedEncadenaAlAnclaOriginalSiElAnclaYaEstaCorrelacionada() {
        UUID idSucursal = UUID.randomUUID();
        UUID idAnclaOriginal = UUID.randomUUID();
        SucursalEntidad sucursal = sucursal(idSucursal, "FMA001");
        when(sucursalRepositorioJpa.findByCodigo("FMA001")).thenReturn(Optional.of(sucursal));
        when(alertaRepositorioJpa.findBySucursal_IdAndTipoAlertaAndEstadoIn(eq(idSucursal), eq("LATENCIA_ALTA"), anyList()))
            .thenReturn(List.of());

        AlertaEntidad alertaIntermedia = mock(AlertaEntidad.class);
        when(alertaIntermedia.getId()).thenReturn(UUID.randomUUID());
        when(alertaIntermedia.getCorrelacionId()).thenReturn(idAnclaOriginal);
        when(alertaRepositorioJpa.buscarPorSucursalTiposYEstados(eq(idSucursal), anyList(), anyList()))
            .thenReturn(List.of(alertaIntermedia));

        adaptador.guardarAlertaRed(new AlertaRed("FMA001", "WARNING", "LATENCIA_ALTA", "Latencia alta", "msg"));

        ArgumentCaptor<AlertaEntidad> captor = ArgumentCaptor.forClass(AlertaEntidad.class);
        verify(alertaRepositorioJpa, times(1)).save(captor.capture());
        assertEquals(idAnclaOriginal, captor.getValue().getCorrelacionId());
    }

    private EquipoEntidad equipoSinSucursal(UUID id) {
        EquipoEntidad equipo = mock(EquipoEntidad.class);
        when(equipo.getId()).thenReturn(id);
        when(equipo.getSucursal()).thenReturn(null);
        return equipo;
    }

    private EquipoEntidad equipoConSucursal(UUID idEquipo, UUID idSucursal) {
        EquipoEntidad equipo = mock(EquipoEntidad.class);
        SucursalEntidad sucursal = sucursal(idSucursal, "FMA001");
        when(equipo.getId()).thenReturn(idEquipo);
        when(equipo.getSucursal()).thenReturn(sucursal);
        return equipo;
    }

    private SucursalEntidad sucursal(UUID id, String codigo) {
        SucursalEntidad sucursal = mock(SucursalEntidad.class);
        when(sucursal.getId()).thenReturn(id);
        when(sucursal.getCodigo()).thenReturn(codigo);
        when(sucursal.isDeTurno()).thenReturn(false);
        return sucursal;
    }
}
