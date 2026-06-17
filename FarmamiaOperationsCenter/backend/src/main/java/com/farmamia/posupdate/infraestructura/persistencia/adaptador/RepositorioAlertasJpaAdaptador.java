package com.farmamia.posupdate.infraestructura.persistencia.adaptador;

import com.farmamia.posupdate.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.posupdate.dominio.modelo.AlertaEquipo;
import com.farmamia.posupdate.dominio.modelo.AlertaRed;
import com.farmamia.posupdate.dominio.modelo.AlertaRegistrada;
import com.farmamia.posupdate.dominio.modelo.FiltroAlertas;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.dominio.puerto.RepositorioAlertas;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.AlertaEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.SucursalEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.UsuarioAppEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.AlertaRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.SucursalRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.UsuarioAppRepositorioJpa;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioAlertasJpaAdaptador implements RepositorioAlertas {

    private static final OffsetDateTime FECHA_NEUTRA = OffsetDateTime.parse("1970-01-01T00:00:00Z");
    private static final UUID UUID_NEUTRO = new UUID(0L, 0L);

    private final EquipoRepositorioJpa equipoRepositorioJpa;
    private final AlertaRepositorioJpa alertaRepositorioJpa;
    private final UsuarioAppRepositorioJpa usuarioAppRepositorioJpa;
    private final SucursalRepositorioJpa sucursalRepositorioJpa;

    public RepositorioAlertasJpaAdaptador(
        EquipoRepositorioJpa equipoRepositorioJpa,
        AlertaRepositorioJpa alertaRepositorioJpa,
        UsuarioAppRepositorioJpa usuarioAppRepositorioJpa,
        SucursalRepositorioJpa sucursalRepositorioJpa
    ) {
        this.equipoRepositorioJpa = equipoRepositorioJpa;
        this.alertaRepositorioJpa = alertaRepositorioJpa;
        this.usuarioAppRepositorioJpa = usuarioAppRepositorioJpa;
        this.sucursalRepositorioJpa = sucursalRepositorioJpa;
    }

    @Override
    public void guardar(AlertaEquipo alerta) {
        EquipoEntidad equipo = equipoRepositorioJpa.findById(alerta.idEquipo())
            .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado: " + alerta.idEquipo()));

        alertaRepositorioJpa.save(new AlertaEntidad(
            equipo,
            severidadOperacional(alerta, equipo),
            alerta.tipoAlerta(),
            alerta.titulo(),
            alerta.mensaje()
        ));
    }

    @Override
    public void guardarAlertaRed(AlertaRed alerta) {
        SucursalEntidad sucursal = sucursalRepositorioJpa.findByCodigo(alerta.codigoSucursal())
            .orElseThrow(() -> new RecursoNoEncontradoException("Farmacia no encontrada: " + alerta.codigoSucursal()));
        alertaRepositorioJpa.save(new AlertaEntidad(sucursal, alerta.severidad(), alerta.tipoAlerta(), alerta.titulo(), alerta.mensaje()));
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
        String estado = minusculaANulo(filtro.estado());
        String severidad = minusculaANulo(filtro.severidad());
        String tipo = minusculaANulo(filtro.tipo());
        String codigoSucursal = minusculaANulo(filtro.codigoSucursal());
        String nombreEquipo = minusculaANulo(filtro.nombreEquipo());
        return alertaRepositorioJpa.buscarConFiltros(
            estado != null,
            nuloAValor(estado),
            severidad != null,
            nuloAValor(severidad),
            tipo != null,
            nuloAValor(tipo),
            filtro.idEquipo() != null,
            filtro.idEquipo() == null ? UUID_NEUTRO : filtro.idEquipo(),
            filtro.idSucursal() != null,
            filtro.idSucursal() == null ? UUID_NEUTRO : filtro.idSucursal(),
            codigoSucursal != null,
            nuloAValor(codigoSucursal),
            nombreEquipo != null,
            nuloAValor(nombreEquipo),
            filtro.fechaDesde() != null,
            filtro.fechaDesde() == null ? FECHA_NEUTRA : filtro.fechaDesde(),
            filtro.fechaHasta() != null,
            filtro.fechaHasta() == null ? FECHA_NEUTRA : filtro.fechaHasta(),
            filtro.eventoDeRed() != null,
            filtro.eventoDeRed() != null && filtro.eventoDeRed(),
            PageRequest.of(filtro.pagina(), filtro.tamano(), aOrden(filtro.orden()))
        )
            .stream()
            .map(this::aDominio)
            .toList();
    }

    @Override
    public Pagina<AlertaRegistrada> listarPaginado(FiltroAlertas filtro) {
        String estado = minusculaANulo(filtro.estado());
        String severidad = minusculaANulo(filtro.severidad());
        String tipo = minusculaANulo(filtro.tipo());
        String codigoSucursal = minusculaANulo(filtro.codigoSucursal());
        String nombreEquipo = minusculaANulo(filtro.nombreEquipo());
        org.springframework.data.domain.Page<AlertaEntidad> pagina = alertaRepositorioJpa.buscarConFiltrosPaginado(
            estado != null,
            nuloAValor(estado),
            severidad != null,
            nuloAValor(severidad),
            tipo != null,
            nuloAValor(tipo),
            filtro.idEquipo() != null,
            filtro.idEquipo() == null ? UUID_NEUTRO : filtro.idEquipo(),
            filtro.idSucursal() != null,
            filtro.idSucursal() == null ? UUID_NEUTRO : filtro.idSucursal(),
            codigoSucursal != null,
            nuloAValor(codigoSucursal),
            nombreEquipo != null,
            nuloAValor(nombreEquipo),
            filtro.fechaDesde() != null,
            filtro.fechaDesde() == null ? FECHA_NEUTRA : filtro.fechaDesde(),
            filtro.fechaHasta() != null,
            filtro.fechaHasta() == null ? FECHA_NEUTRA : filtro.fechaHasta(),
            filtro.eventoDeRed() != null,
            filtro.eventoDeRed() != null && filtro.eventoDeRed(),
            PageRequest.of(filtro.pagina(), filtro.tamano(), aOrden(filtro.orden()))
        );

        return new Pagina<>(
            pagina.getContent().stream().map(this::aDominio).toList(),
            pagina.getNumber(),
            pagina.getSize(),
            pagina.getTotalElements(),
            pagina.getTotalPages(),
            pagina.hasNext()
        );
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
        SucursalEntidad sucursal = equipo != null ? equipo.getSucursal() : alerta.getSucursal();
        return new AlertaRegistrada(
            alerta.getId(),
            equipo != null ? equipo.getId() : null,
            equipo != null ? equipo.getNombreEquipo() : null,
            sucursal != null ? sucursal.getId() : null,
            sucursal != null ? sucursal.getCodigo() : alerta.getCodigoSucursalRed(),
            alerta.getSeveridad(),
            alerta.getTipoAlerta(),
            alerta.getTitulo(),
            alerta.getMensaje(),
            alerta.getEstado(),
            alerta.getAbiertaEn(),
            alerta.getReconocidaPor() == null ? null : alerta.getReconocidaPor().getUsuario(),
            alerta.getReconocidaEn(),
            alerta.getCerradaPor() == null ? null : alerta.getCerradaPor().getUsuario(),
            alerta.getCerradaEn(),
            alerta.isEventoDeRed()
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

    private String severidadOperacional(AlertaEquipo alerta, EquipoEntidad equipo) {
        if (equipo.getSucursal() == null || !equipo.getSucursal().isDeTurno()) {
            return alerta.severidad();
        }

        return switch (alerta.tipoAlerta()) {
            case "UPDATE_FAILED", "ROLLBACK_FAILED", "POS_OFFLINE", "DEVICE_OFFLINE", "HEARTBEAT_STALE", "CAMPANA_PENDIENTE_FUERA_VENTANA" -> "CRITICAL";
            case "ROLLBACK_COMPLETED" -> "HIGH";
            default -> alerta.severidad();
        };
    }

    private String minusculaANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase(Locale.ROOT);
    }

    private String nuloAValor(String valor) {
        return valor == null ? "" : valor;
    }
}
