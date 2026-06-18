package com.farmamia.posupdate.infraestructura.persistencia.adaptador;

import com.farmamia.posupdate.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.posupdate.dominio.modelo.AsignacionEquipoSucursal;
import com.farmamia.posupdate.dominio.modelo.DatosRegistroAgente;
import com.farmamia.posupdate.dominio.modelo.Equipo;
import com.farmamia.posupdate.dominio.modelo.FiltroEquipos;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.dominio.puerto.RepositorioEquipos;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.SucursalEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.SucursalRepositorioJpa;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Repository
public class RepositorioEquiposJpaAdaptador implements RepositorioEquipos {

    private static final OffsetDateTime FECHA_NEUTRA = OffsetDateTime.parse("1970-01-01T00:00:00Z");

    private final EquipoRepositorioJpa equipoRepositorioJpa;
    private final SucursalRepositorioJpa sucursalRepositorioJpa;

    public RepositorioEquiposJpaAdaptador(
        EquipoRepositorioJpa equipoRepositorioJpa,
        SucursalRepositorioJpa sucursalRepositorioJpa
    ) {
        this.equipoRepositorioJpa = equipoRepositorioJpa;
        this.sucursalRepositorioJpa = sucursalRepositorioJpa;
    }

    @Override
    public Equipo registrarOActualizar(UUID idSucursal, DatosRegistroAgente datosRegistro) {
        SucursalEntidad sucursal = null;
        if (idSucursal != null) {
            sucursal = sucursalRepositorioJpa.findById(idSucursal)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada: " + idSucursal));
        }
        final SucursalEntidad sucursalRegistro = sucursal;

        EquipoEntidad equipo = equipoRepositorioJpa.findByNombreEquipo(datosRegistro.nombreEquipo())
            .orElseGet(() -> new EquipoEntidad(sucursalRegistro, datosRegistro.nombreEquipo(), datosRegistro.rutaPos()));

        equipo.actualizarRegistro(
            sucursal,
            datosRegistro.direccionIp(),
            datosRegistro.direccionMac(),
            datosRegistro.versionWindows(),
            datosRegistro.versionAgente(),
            datosRegistro.versionPos(),
            datosRegistro.rutaPos()
        );

        return aDominio(equipoRepositorioJpa.save(equipo));
    }

    @Override
    public Optional<Equipo> buscarPorId(UUID idEquipo) {
        return equipoRepositorioJpa.findById(idEquipo).map(this::aDominio);
    }

    @Override
    public List<Equipo> listar() {
        return equipoRepositorioJpa.findAll()
            .stream()
            .sorted(Comparator.comparing(EquipoEntidad::getNombreEquipo))
            .map(this::aDominio)
            .toList();
    }

    @Override
    public Pagina<Equipo> listarPaginado(FiltroEquipos filtro) {
        String q = minusculaANulo(filtro.q());
        String estado = minusculaANulo(filtro.estado());
        String codigoSucursal = minusculaANulo(filtro.codigoSucursal());
        String versionPos = minusculaANulo(filtro.versionPos());
        String versionAgente = minusculaANulo(filtro.versionAgente());
        org.springframework.data.domain.Page<EquipoEntidad> pagina = equipoRepositorioJpa.buscarConFiltros(
            q != null,
            nuloAValor(q),
            estado != null,
            nuloAValor(estado),
            codigoSucursal != null,
            nuloAValor(codigoSucursal),
            versionPos != null,
            nuloAValor(versionPos),
            versionAgente != null,
            nuloAValor(versionAgente),
            filtro.ultimoLatidoDesde() != null,
            filtro.ultimoLatidoDesde() == null ? FECHA_NEUTRA : filtro.ultimoLatidoDesde(),
            filtro.ultimoLatidoHasta() != null,
            filtro.ultimoLatidoHasta() == null ? FECHA_NEUTRA : filtro.ultimoLatidoHasta(),
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
    public List<Equipo> listarHuerfanos() {
        return equipoRepositorioJpa.findBySucursalIsNullOrderByNombreEquipoAsc()
            .stream()
            .map(this::aDominio)
            .toList();
    }

    @Override
    public long contarHuerfanosPorIds(Set<UUID> idsEquipos) {
        return idsEquipos.isEmpty() ? 0 : equipoRepositorioJpa.countByIdInAndSucursalIsNull(idsEquipos);
    }

    @Override
    @Transactional
    public void asignarSucursales(List<AsignacionEquipoSucursal> asignaciones) {
        if (asignaciones == null || asignaciones.isEmpty()) {
            return;
        }

        // 1. Extraer los IDs únicos involucrados en la solicitud
        List<UUID> equipoIds = asignaciones.stream()
                .map(AsignacionEquipoSucursal::idEquipo)
                .toList();
                
        List<UUID> sucursalIds = asignaciones.stream()
                .map(AsignacionEquipoSucursal::idSucursal)
                .distinct()
                .toList();

        // 2. Realizar solo dos consultas masivas (SELECT IN) para traer toda la información
        java.util.Map<UUID, EquipoEntidad> equiposMap = equipoRepositorioJpa.findAllById(equipoIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(EquipoEntidad::getId, e -> e));

        java.util.Map<UUID, SucursalEntidad> sucursalesMap = sucursalRepositorioJpa.findAllById(sucursalIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(SucursalEntidad::getId, s -> s));

        // 3. Vincular las relaciones en memoria reduciendo la latencia de red con la BD
        for (AsignacionEquipoSucursal asignacion : asignaciones) {
            EquipoEntidad equipo = equiposMap.get(asignacion.idEquipo());
            if (equipo == null) {
                throw new RecursoNoEncontradoException("Equipo no encontrado: " + asignacion.idEquipo());
            }

            SucursalEntidad sucursal = sucursalesMap.get(asignacion.idSucursal());
            if (sucursal == null) {
                throw new RecursoNoEncontradoException("Sucursal no encontrada: " + asignacion.idSucursal());
            }

            equipo.asignarSucursal(sucursal);
        }

        // 4. Guardar los cambios actualizados en lote
        equipoRepositorioJpa.saveAll(equiposMap.values());
    }

    @Override
    public void registrarLatido(UUID idEquipo, String versionPos) {
        EquipoEntidad equipo = equipoRepositorioJpa.findById(idEquipo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado: " + idEquipo));
        equipo.registrarLatido(versionPos);
    }

    @Override
    public void actualizarVersionPos(UUID idEquipo, String versionPos) {
        EquipoEntidad equipo = equipoRepositorioJpa.findById(idEquipo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado: " + idEquipo));
        equipo.actualizarVersionPos(versionPos);
    }

    private Equipo aDominio(EquipoEntidad entidad) {
        SucursalEntidad sucursal = entidad.getSucursal();
        return new Equipo(
            entidad.getId(),
            sucursal == null ? null : sucursal.getId(),
            sucursal == null ? "" : sucursal.getCodigo(),
            sucursal == null ? "SIN ASIGNAR" : sucursal.getNombre(),
            entidad.getNombreEquipo(),
            entidad.getDireccionIp(),
            entidad.getDireccionMac(),
            entidad.getVersionWindows(),
            entidad.getVersionAgente(),
            entidad.getVersionPos(),
            entidad.getRutaPos(),
            entidad.getEstado(),
            entidad.getUltimoLatidoEn(),
            entidad.getRegistradoEn(),
            entidad.getActualizadoEn()
        );
    }

    private Sort aOrden(String orden) {
        String[] partes = orden == null ? new String[0] : orden.split(",", 2);
        String campo = partes.length > 0 ? partes[0] : "nombreEquipo";
        Sort.Direction direccion = partes.length > 1 && "desc".equalsIgnoreCase(partes[1])
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        return Sort.by(direccion, switch (campo) {
            case "lastHeartbeatAt", "ultimoLatidoEn" -> "ultimoLatidoEn";
            case "posVersion", "versionPos" -> "versionPos";
            case "agentVersion", "versionAgente" -> "versionAgente";
            case "status", "estado" -> "estado";
            case "updatedAt", "actualizadoEn" -> "actualizadoEn";
            default -> "nombreEquipo";
        });
    }

    private String minusculaANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase(Locale.ROOT);
    }

    private String nuloAValor(String valor) {
        return valor == null ? "" : valor;
    }
}
