package com.farmamia.operations.aplicacion.casouso;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.modelo.DatosGrupoTrx;
import com.farmamia.operations.dominio.modelo.DetalleGrupoTrx;
import com.farmamia.operations.dominio.modelo.EstadoGrupoTrx;
import com.farmamia.operations.dominio.modelo.FiltroGruposTrx;
import com.farmamia.operations.dominio.modelo.GrupoTrx;
import com.farmamia.operations.dominio.modelo.Pagina;
import com.farmamia.operations.dominio.puerto.RepositorioGruposTrx;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestionarGruposTrxCasoUso {

    private static final int MAXIMO_EQUIPOS_OFICIAL = 100;

    private final RepositorioGruposTrx repositorioGruposTrx;

    public GestionarGruposTrxCasoUso(RepositorioGruposTrx repositorioGruposTrx) {
        this.repositorioGruposTrx = repositorioGruposTrx;
    }

    @Transactional
    public GrupoTrx crear(DatosGrupoTrx datos) {
        DatosGrupoTrx normalizados = normalizar(datos, true);
        repositorioGruposTrx.buscarPorCodigo(normalizados.codigo())
            .ifPresent(grupo -> {
                throw new IllegalArgumentException("Ya existe un Grupo TRX con codigo " + normalizados.codigo());
            });
        return repositorioGruposTrx.crear(normalizados);
    }

    @Transactional
    public GrupoTrx actualizar(UUID id, DatosGrupoTrx datos) {
        obtener(id);
        DatosGrupoTrx normalizados = normalizar(datos, false);
        if (normalizados.codigo() != null) {
            repositorioGruposTrx.buscarPorCodigo(normalizados.codigo())
                .filter(grupo -> !grupo.id().equals(id))
                .ifPresent(grupo -> {
                    throw new IllegalArgumentException("Ya existe un Grupo TRX con codigo " + normalizados.codigo());
                });
        }
        return repositorioGruposTrx.actualizar(id, normalizados);
    }

    @Transactional
    public GrupoTrx pausar(UUID id) {
        GrupoTrx grupo = obtenerGrupo(id);
        exigirNoRetirado(grupo);
        return repositorioGruposTrx.cambiarEstado(id, EstadoGrupoTrx.PAUSADO);
    }

    @Transactional
    public GrupoTrx reanudar(UUID id) {
        GrupoTrx grupo = obtenerGrupo(id);
        exigirNoRetirado(grupo);
        return repositorioGruposTrx.cambiarEstado(id, EstadoGrupoTrx.ACTIVO);
    }

    @Transactional
    public GrupoTrx retirar(UUID id) {
        obtener(id);
        return repositorioGruposTrx.cambiarEstado(id, EstadoGrupoTrx.RETIRADO);
    }

    @Transactional
    public GrupoTrx asignarEquipo(UUID idGrupo, UUID idEquipo) {
        GrupoTrx grupo = obtenerGrupo(idGrupo);
        if (grupo.estado() == EstadoGrupoTrx.PAUSADO) {
            throw new IllegalArgumentException("No se pueden asignar equipos a un Grupo TRX pausado.");
        }
        if (grupo.estado() == EstadoGrupoTrx.RETIRADO || !grupo.activo()) {
            throw new IllegalArgumentException("No se pueden asignar equipos a un Grupo TRX retirado o inactivo.");
        }
        if (grupo.equiposAsignados() >= grupo.maximoEquipos()) {
            throw new IllegalArgumentException("El Grupo TRX ya alcanzo el maximo de " + grupo.maximoEquipos() + " equipos.");
        }
        return repositorioGruposTrx.asignarEquipo(idGrupo, idEquipo);
    }

    @Transactional
    public GrupoTrx quitarEquipo(UUID idGrupo, UUID idEquipo) {
        obtener(idGrupo);
        return repositorioGruposTrx.quitarEquipo(idGrupo, idEquipo);
    }

    @Transactional(readOnly = true)
    public DetalleGrupoTrx obtener(UUID id) {
        return repositorioGruposTrx.buscarDetallePorId(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Grupo TRX no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<GrupoTrx> listar() {
        return repositorioGruposTrx.listar();
    }

    @Transactional(readOnly = true)
    public Pagina<GrupoTrx> listarPaginado(FiltroGruposTrx filtro) {
        return repositorioGruposTrx.listarPaginado(new FiltroGruposTrx(
            limpiar(filtro.codigo()),
            limpiar(filtro.estado()),
            filtro.activo(),
            Math.max(0, filtro.pagina()),
            Math.max(1, Math.min(filtro.tamano(), 200)),
            limpiar(filtro.orden()) == null ? "codigo,asc" : filtro.orden()
        ));
    }

    private GrupoTrx obtenerGrupo(UUID id) {
        return obtener(id).grupo();
    }

    private DatosGrupoTrx normalizar(DatosGrupoTrx datos, boolean crear) {
        String codigo = limpiar(datos.codigo());
        String nombre = limpiar(datos.nombre());
        if (crear && codigo == null) {
            throw new IllegalArgumentException("El codigo del Grupo TRX es obligatorio.");
        }
        if (crear && nombre == null) {
            throw new IllegalArgumentException("El nombre del Grupo TRX es obligatorio.");
        }
        Integer maximo = datos.maximoEquipos() == null ? MAXIMO_EQUIPOS_OFICIAL : datos.maximoEquipos();
        if (maximo < 1 || maximo > MAXIMO_EQUIPOS_OFICIAL) {
            throw new IllegalArgumentException("El maximo de equipos por Grupo TRX debe estar entre 1 y 100.");
        }
        return new DatosGrupoTrx(
            codigo == null ? null : codigo.toLowerCase(Locale.ROOT),
            nombre,
            limpiar(datos.descripcion()),
            maximo,
            datos.activo()
        );
    }

    private void exigirNoRetirado(GrupoTrx grupo) {
        if (grupo.estado() == EstadoGrupoTrx.RETIRADO) {
            throw new IllegalArgumentException("El Grupo TRX retirado no puede cambiar a este estado.");
        }
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
