package com.farmamia.operations.aplicacion.casouso;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.aplicacion.excepcion.ConflictoIdempotenciaException;
import com.farmamia.operations.dominio.modelo.AlertaEquipo;
import com.farmamia.operations.dominio.modelo.DatosEventoAgente;
import com.farmamia.operations.dominio.modelo.EventoActualizacion;
import com.farmamia.operations.dominio.modelo.ResultadoActualizacion;
import com.farmamia.operations.dominio.puerto.RepositorioAlertas;
import com.farmamia.operations.dominio.puerto.RepositorioEquipos;
import com.farmamia.operations.dominio.puerto.RepositorioEventosActualizacion;
import com.farmamia.operations.dominio.puerto.RepositorioObjetivosDespliegue;
import jakarta.transaction.Transactional;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RegistrarEventoAgenteCasoUso {

    private static final String ESTADO_COMPLETADO = "COMPLETED";
    private static final String ESTADO_ROLLBACK_COMPLETADO = "ROLLBACK_COMPLETED";
    private static final String ESTADO_ROLLBACK_FALLIDO = "ROLLBACK_FAILED";
    private static final String EVENTO_RESULTADO_ACTUALIZACION = "UPDATE_COMPLETED";
    private static final String EVENTO_ROLLBACK_COMPLETADO = "ROLLBACK_COMPLETED";
    private static final String EVENTO_FALLO = "FAILED";

    private final RepositorioEquipos repositorioEquipos;
    private final RepositorioObjetivosDespliegue repositorioObjetivosDespliegue;
    private final RepositorioEventosActualizacion repositorioEventosActualizacion;
    private final RepositorioAlertas repositorioAlertas;

    public RegistrarEventoAgenteCasoUso(
        RepositorioEquipos repositorioEquipos,
        RepositorioObjetivosDespliegue repositorioObjetivosDespliegue,
        RepositorioEventosActualizacion repositorioEventosActualizacion,
        RepositorioAlertas repositorioAlertas
    ) {
        this.repositorioEquipos = repositorioEquipos;
        this.repositorioObjetivosDespliegue = repositorioObjetivosDespliegue;
        this.repositorioEventosActualizacion = repositorioEventosActualizacion;
        this.repositorioAlertas = repositorioAlertas;
    }

    @Transactional
    public void registrarEvento(UUID idEquipo, DatosEventoAgente datos) {
        validarEquipo(idEquipo);
        validarObjetivoOpcional(idEquipo, datos.idObjetivoDespliegue());

        EventoActualizacion evento = new EventoActualizacion(
            idEquipo,
            datos.idObjetivoDespliegue(),
            normalizarIdempotencyKey(datos.idempotencyKey()),
            datos.tipoEvento(),
            datos.mensajeEvento(),
            datos.versionAnterior(),
            datos.versionNueva(),
            datos.metadatos()
        );

        if (esRepetidoIdempotente(evento)) {
            return;
        }

        repositorioEventosActualizacion.guardar(evento);
    }

    @Transactional
    public void registrarResultadoActualizacion(UUID idEquipo, ResultadoActualizacion resultado) {
        validarEquipo(idEquipo);

        ResultadoActualizacion resultadoPersistible = new ResultadoActualizacion(
            resultado.idObjetivoDespliegue(),
            normalizarIdempotencyKey(resultado.idempotencyKey()),
            resultado.estado(),
            resultado.versionAnterior(),
            resultado.versionNueva(),
            esFallo(resultado.estado()) ? resultado.mensaje() : null
        );

        EventoActualizacion eventoResultado = new EventoActualizacion(
            idEquipo,
            resultado.idObjetivoDespliegue(),
            normalizarIdempotencyKey(resultado.idempotencyKey()),
            tipoEventoResultado(resultado.estado()),
            resultado.mensaje(),
            resultado.versionAnterior(),
            resultado.versionNueva(),
            Map.of("status", resultado.estado())
        );

        if (esRepetidoIdempotente(eventoResultado)) {
            return;
        }

        repositorioObjetivosDespliegue.registrarResultado(idEquipo, resultadoPersistible);

        if (ESTADO_COMPLETADO.equals(resultado.estado())) {
            repositorioEquipos.actualizarVersionPos(idEquipo, resultado.versionNueva());
        }

        repositorioEventosActualizacion.guardar(eventoResultado);

        generarAlertaSiCorresponde(idEquipo, resultado);
    }

    private boolean esRepetidoIdempotente(EventoActualizacion evento) {
        if (evento.idempotencyKey() == null) {
            return false;
        }

        if (!repositorioEventosActualizacion.existeConIdempotencia(evento.idEquipo(), evento.idempotencyKey())) {
            return false;
        }

        if (!repositorioEventosActualizacion.coincideConIdempotencia(evento)) {
            throw new ConflictoIdempotenciaException(
                "La idempotencyKey ya fue usada con un payload diferente: " + evento.idempotencyKey()
            );
        }

        return true;
    }

    private String normalizarIdempotencyKey(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void validarEquipo(UUID idEquipo) {
        repositorioEquipos.buscarPorId(idEquipo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado: " + idEquipo));
    }

    private void validarObjetivoOpcional(UUID idEquipo, UUID idObjetivoDespliegue) {
        if (idObjetivoDespliegue == null) {
            return;
        }

        repositorioObjetivosDespliegue.validarPerteneceAEquipo(idObjetivoDespliegue, idEquipo);
    }

    private String tipoEventoResultado(String estado) {
        if (ESTADO_COMPLETADO.equals(estado)) {
            return EVENTO_RESULTADO_ACTUALIZACION;
        }

        if (ESTADO_ROLLBACK_COMPLETADO.equals(estado)) {
            return EVENTO_ROLLBACK_COMPLETADO;
        }

        return EVENTO_FALLO;
    }

    private boolean esFallo(String estado) {
        return !ESTADO_COMPLETADO.equals(estado) && !ESTADO_ROLLBACK_COMPLETADO.equals(estado);
    }

    private void generarAlertaSiCorresponde(UUID idEquipo, ResultadoActualizacion resultado) {
        if (ESTADO_ROLLBACK_COMPLETADO.equals(resultado.estado())) {
            repositorioAlertas.guardar(new AlertaEquipo(
                idEquipo,
                "WARNING",
                "ROLLBACK_COMPLETED",
                "Rollback POS completado",
                resultado.mensaje()
            ));
            return;
        }

        if (!esFallo(resultado.estado())) {
            return;
        }

        String tipoAlerta = ESTADO_ROLLBACK_FALLIDO.equals(resultado.estado())
            ? "ROLLBACK_FAILED"
            : "UPDATE_FAILED";

        repositorioAlertas.guardar(new AlertaEquipo(
            idEquipo,
            "CRITICAL",
            tipoAlerta,
            "Fallo de actualizacion POS",
            resultado.mensaje()
        ));
    }
}
