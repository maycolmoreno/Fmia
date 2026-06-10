package com.farmamia.operations.aplicacion.casouso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.farmamia.operations.dominio.modelo.AlertaEquipo;
import com.farmamia.operations.dominio.modelo.AlertaRegistrada;
import com.farmamia.operations.dominio.modelo.DatosRegistroAgente;
import com.farmamia.operations.dominio.modelo.Equipo;
import com.farmamia.operations.dominio.modelo.EventoActualizacion;
import com.farmamia.operations.dominio.modelo.EventoActualizacionRegistrado;
import com.farmamia.operations.dominio.modelo.FiltroAlertas;
import com.farmamia.operations.dominio.modelo.ResultadoActualizacion;
import com.farmamia.operations.dominio.puerto.RepositorioAlertas;
import com.farmamia.operations.dominio.puerto.RepositorioEquipos;
import com.farmamia.operations.dominio.puerto.RepositorioEventosActualizacion;
import com.farmamia.operations.dominio.puerto.RepositorioObjetivosDespliegue;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegistrarEventoAgenteCasoUsoTest {

    @Test
    void resultadoFallidoActualizaObjetivoRegistraEventoGeneraAlertaYNoCambiaVersionPos() {
        UUID idEquipo = UUID.randomUUID();
        UUID idSucursal = UUID.randomUUID();
        UUID idObjetivo = UUID.randomUUID();
        String versionActual = "2026.06.0";
        String versionFallida = "2026.06.2-fail";
        String mensaje = "Validacion final fallida: el ZIP no contiene Zabyca.Pos.Desktop.exe.";

        RepositorioEquiposEnMemoria repositorioEquipos = new RepositorioEquiposEnMemoria(
            new Equipo(
                idEquipo,
                idSucursal,
                "FMA-DEMO-001",
                "Sucursal demo",
                "POS-DEMO-001",
                "192.168.10.25",
                "00-11-22-33-44-55",
                "Windows 11",
                "0.1.0-demo",
                versionActual,
                "C:\\Farmamia\\POS",
                "ONLINE",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                OffsetDateTime.now()
            )
        );
        RepositorioObjetivosEnMemoria repositorioObjetivos = new RepositorioObjetivosEnMemoria(idObjetivo);
        RepositorioEventosEnMemoria repositorioEventos = new RepositorioEventosEnMemoria();
        RepositorioAlertasEnMemoria repositorioAlertas = new RepositorioAlertasEnMemoria(idSucursal);
        RegistrarEventoAgenteCasoUso casoUso = new RegistrarEventoAgenteCasoUso(
            repositorioEquipos,
            repositorioObjetivos,
            repositorioEventos,
            repositorioAlertas
        );
        ConsultarAlertasCasoUso consultarAlertas = new ConsultarAlertasCasoUso(repositorioAlertas);

        casoUso.registrarResultadoActualizacion(idEquipo, new ResultadoActualizacion(
            idObjetivo,
            "FAILED",
            versionActual,
            versionFallida,
            mensaje
        ));

        assertEquals("FAILED", repositorioObjetivos.resultadoRegistrado.estado());
        assertEquals(mensaje, repositorioObjetivos.resultadoRegistrado.mensaje());
        assertNull(repositorioEquipos.versionActualizada);
        assertEquals(1, repositorioEventos.eventos.size());
        assertEquals("FAILED", repositorioEventos.eventos.getFirst().tipoEvento());
        assertEquals(1, repositorioAlertas.alertas.size());

        List<AlertaRegistrada> alertas = consultarAlertas.listarRecientes(100);
        assertFalse(alertas.isEmpty());
        assertEquals("CRITICAL", alertas.getFirst().severidad());
        assertEquals("UPDATE_FAILED", alertas.getFirst().tipoAlerta());
        assertEquals(mensaje, alertas.getFirst().mensaje());
    }

    @Test
    void resultadoCompletadoActualizaObjetivoVersionPosRegistraEventoYNoGeneraAlerta() {
        UUID idEquipo = UUID.randomUUID();
        UUID idSucursal = UUID.randomUUID();
        UUID idObjetivo = UUID.randomUUID();
        String versionActual = "2026.06.1";
        String versionNueva = "2026.06.2-success";

        RepositorioEquiposEnMemoria repositorioEquipos = new RepositorioEquiposEnMemoria(
            new Equipo(
                idEquipo,
                idSucursal,
                "FMA-DEMO-001",
                "Sucursal demo",
                "POS-DEMO-SUCCESS-001",
                "192.168.10.26",
                "00-11-22-33-44-66",
                "Windows 11",
                "0.1.0-demo",
                versionActual,
                "C:\\Farmamia\\POS",
                "ONLINE",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                OffsetDateTime.now()
            )
        );
        RepositorioObjetivosEnMemoria repositorioObjetivos = new RepositorioObjetivosEnMemoria(idObjetivo);
        RepositorioEventosEnMemoria repositorioEventos = new RepositorioEventosEnMemoria();
        RepositorioAlertasEnMemoria repositorioAlertas = new RepositorioAlertasEnMemoria(idSucursal);
        RegistrarEventoAgenteCasoUso casoUso = new RegistrarEventoAgenteCasoUso(
            repositorioEquipos,
            repositorioObjetivos,
            repositorioEventos,
            repositorioAlertas
        );

        casoUso.registrarResultadoActualizacion(idEquipo, new ResultadoActualizacion(
            idObjetivo,
            "COMPLETED",
            versionActual,
            versionNueva,
            "Actualizacion demo completada correctamente."
        ));

        assertEquals("COMPLETED", repositorioObjetivos.resultadoRegistrado.estado());
        assertNull(repositorioObjetivos.resultadoRegistrado.mensaje());
        assertEquals(versionNueva, repositorioEquipos.versionActualizada);
        assertEquals(1, repositorioEventos.eventos.size());
        assertEquals("UPDATE_COMPLETED", repositorioEventos.eventos.getFirst().tipoEvento());
        assertEquals(0, repositorioAlertas.alertas.size());
    }

    private static final class RepositorioEquiposEnMemoria implements RepositorioEquipos {

        private final Equipo equipo;
        private String versionActualizada;

        private RepositorioEquiposEnMemoria(Equipo equipo) {
            this.equipo = equipo;
        }

        @Override
        public Equipo registrarOActualizar(UUID idSucursal, DatosRegistroAgente datosRegistro) {
            return equipo;
        }

        @Override
        public Optional<Equipo> buscarPorId(UUID idEquipo) {
            return equipo.id().equals(idEquipo) ? Optional.of(equipo) : Optional.empty();
        }

        @Override
        public List<Equipo> listar() {
            return List.of(equipo);
        }

        @Override
        public void registrarLatido(UUID idEquipo, String versionPos) {
        }

        @Override
        public void actualizarVersionPos(UUID idEquipo, String versionPos) {
            this.versionActualizada = versionPos;
        }
    }

    private static final class RepositorioObjetivosEnMemoria implements RepositorioObjetivosDespliegue {

        private final UUID idObjetivo;
        private ResultadoActualizacion resultadoRegistrado;

        private RepositorioObjetivosEnMemoria(UUID idObjetivo) {
            this.idObjetivo = idObjetivo;
        }

        @Override
        public void validarPerteneceAEquipo(UUID idObjetivoDespliegue, UUID idEquipo) {
            if (!idObjetivo.equals(idObjetivoDespliegue)) {
                throw new IllegalArgumentException("Objetivo inesperado");
            }
        }

        @Override
        public void registrarResultado(UUID idEquipo, ResultadoActualizacion resultado) {
            validarPerteneceAEquipo(resultado.idObjetivoDespliegue(), idEquipo);
            this.resultadoRegistrado = resultado;
        }
    }

    private static final class RepositorioEventosEnMemoria implements RepositorioEventosActualizacion {

        private final List<EventoActualizacion> eventos = new ArrayList<>();

        @Override
        public void guardar(EventoActualizacion evento) {
            eventos.add(evento);
        }

        @Override
        public List<EventoActualizacionRegistrado> listarRecientes(int limite) {
            return List.of();
        }

        @Override
        public List<EventoActualizacionRegistrado> listarRecientesPorEquipo(UUID idEquipo, int limite) {
            return List.of();
        }
    }

    private static final class RepositorioAlertasEnMemoria implements RepositorioAlertas {

        private final UUID idSucursal;
        private final List<AlertaEquipo> alertas = new ArrayList<>();

        private RepositorioAlertasEnMemoria(UUID idSucursal) {
            this.idSucursal = idSucursal;
        }

        @Override
        public void guardar(AlertaEquipo alerta) {
            alertas.add(alerta);
        }

        @Override
        public List<AlertaRegistrada> listarRecientes(int limite) {
            return alertas.stream()
                .limit(limite)
                .map(alerta -> new AlertaRegistrada(
                    UUID.randomUUID(),
                    alerta.idEquipo(),
                    "POS-DEMO-001",
                    idSucursal,
                    "FMA-DEMO-001",
                    alerta.severidad(),
                    alerta.tipoAlerta(),
                    alerta.titulo(),
                    alerta.mensaje(),
                    "OPEN",
                    OffsetDateTime.now(),
                    null,
                    null,
                    null,
                    null
                ))
                .toList();
        }

        @Override
        public List<AlertaRegistrada> listarConFiltros(FiltroAlertas filtro) {
            return listarRecientes(filtro.tamano());
        }

        @Override
        public AlertaRegistrada reconocer(UUID idAlerta, String usuarioActor) {
            throw new UnsupportedOperationException("No requerido en esta prueba");
        }

        @Override
        public AlertaRegistrada cerrar(UUID idAlerta, String usuarioActor) {
            throw new UnsupportedOperationException("No requerido en esta prueba");
        }
    }
}
