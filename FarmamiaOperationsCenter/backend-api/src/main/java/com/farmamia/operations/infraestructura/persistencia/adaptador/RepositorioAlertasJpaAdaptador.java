package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.modelo.AlertaEquipo;
import com.farmamia.operations.dominio.modelo.AlertaRegistrada;
import com.farmamia.operations.dominio.modelo.FiltroAlertas;
import com.farmamia.operations.dominio.puerto.RepositorioAlertas;
import com.farmamia.operations.infraestructura.persistencia.entidad.AlertaEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.SucursalEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.UsuarioAppEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.AlertaRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.UsuarioAppRepositorioJpa;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioAlertasJpaAdaptador implements RepositorioAlertas {

    private final EquipoRepositorioJpa equipoRepositorioJpa;
    private final AlertaRepositorioJpa alertaRepositorioJpa;
    private final UsuarioAppRepositorioJpa usuarioAppRepositorioJpa;

    public RepositorioAlertasJpaAdaptador(
        EquipoRepositorioJpa equipoRepositorioJpa,
        AlertaRepositorioJpa alertaRepositorioJpa,
        UsuarioAppRepositorioJpa usuarioAppRepositorioJpa
    ) {
        this.equipoRepositorioJpa = equipoRepositorioJpa;
        this.alertaRepositorioJpa = alertaRepositorioJpa;
        this.usuarioAppRepositorioJpa = usuarioAppRepositorioJpa;
    }

    @Override
    public void guardar(AlertaEquipo alerta) {
        EquipoEntidad equipo = equipoRepositorioJpa.findById(alerta.idEquipo())
            .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado: " + alerta.idEquipo()));

        alertaRepositorioJpa.save(new AlertaEntidad(
            equipo,
            alerta.severidad(),
            alerta.tipoAlerta(),
            alerta.titulo(),
            alerta.mensaje()
        ));
    }

    @Override
    public List<AlertaRegistrada> listarRecientes(int limite) {
        return alertaRepositorioJpa.findByOrderByAbiertaEnDesc(PageRequest.of(0, limite))
            .stream()
            .map(this::aDominio)
            .toList();
    }

    @Override
    public List<AlertaRegistrada> listarConFiltros(FiltroAlertas filtro) {
        return alertaRepositorioJpa.buscarConFiltros(
            filtro.estado(),
            filtro.severidad(),
            filtro.tipo(),
            filtro.idEquipo(),
            filtro.idSucursal(),
            filtro.codigoSucursal(),
            filtro.nombreEquipo(),
            filtro.fechaDesde(),
            filtro.fechaHasta(),
            PageRequest.of(filtro.pagina(), filtro.tamano(), aOrden(filtro.orden()))
        )
            .stream()
            .map(this::aDominio)
            .toList();
    }

    @Override
    public AlertaRegistrada reconocer(UUID idAlerta, String usuarioActor) {
        AlertaEntidad alerta = buscarAlerta(idAlerta);
        alerta.reconocer(buscarUsuario(usuarioActor));
        return aDominio(alerta);
    }

    @Override
    public AlertaRegistrada cerrar(UUID idAlerta, String usuarioActor) {
        AlertaEntidad alerta = buscarAlerta(idAlerta);
        alerta.cerrar(buscarUsuario(usuarioActor));
        return aDominio(alerta);
    }

    private AlertaRegistrada aDominio(AlertaEntidad alerta) {
        EquipoEntidad equipo = alerta.getEquipo();
        SucursalEntidad sucursal = equipo.getSucursal();
        return new AlertaRegistrada(
            alerta.getId(),
            equipo.getId(),
            equipo.getNombreEquipo(),
            sucursal.getId(),
            sucursal.getCodigo(),
            alerta.getSeveridad(),
            alerta.getTipoAlerta(),
            alerta.getTitulo(),
            alerta.getMensaje(),
            alerta.getEstado(),
            alerta.getAbiertaEn(),
            alerta.getReconocidaPor() == null ? null : alerta.getReconocidaPor().getUsuario(),
            alerta.getReconocidaEn(),
            alerta.getCerradaPor() == null ? null : alerta.getCerradaPor().getUsuario(),
            alerta.getCerradaEn()
        );
    }

    private AlertaEntidad buscarAlerta(UUID idAlerta) {
        return alertaRepositorioJpa.findById(idAlerta)
            .orElseThrow(() -> new RecursoNoEncontradoException("Alerta no encontrada: " + idAlerta));
    }

    private UsuarioAppEntidad buscarUsuario(String usuario) {
        return usuarioAppRepositorioJpa.findByUsuario(usuario)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario administrativo no encontrado: " + usuario));
    }

    private Sort aOrden(String orden) {
        if (orden == null || orden.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "abiertaEn");
        }

        String[] partes = orden.split(",");
        String campo = switch (partes[0].trim()) {
            case "severity", "severidad" -> "severidad";
            case "status", "estado" -> "estado";
            case "type", "tipo", "alertType" -> "tipoAlerta";
            case "openedAt", "date", "fecha", "abiertaEn" -> "abiertaEn";
            default -> "abiertaEn";
        };
        Sort.Direction direccion = partes.length > 1 && "asc".equalsIgnoreCase(partes[1].trim())
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        return Sort.by(direccion, campo);
    }
}
