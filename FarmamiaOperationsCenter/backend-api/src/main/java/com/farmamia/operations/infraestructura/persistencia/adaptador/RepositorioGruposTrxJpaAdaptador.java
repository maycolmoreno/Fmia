package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.modelo.DatosGrupoTrx;
import com.farmamia.operations.dominio.modelo.DetalleGrupoTrx;
import com.farmamia.operations.dominio.modelo.EquipoGrupoTrx;
import com.farmamia.operations.dominio.modelo.EstadoGrupoTrx;
import com.farmamia.operations.dominio.modelo.FiltroGruposTrx;
import com.farmamia.operations.dominio.modelo.GrupoTrx;
import com.farmamia.operations.dominio.modelo.Pagina;
import com.farmamia.operations.dominio.puerto.RepositorioGruposTrx;
import com.farmamia.operations.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.EquipoGrupoTrxEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.GrupoTrxEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.SucursalEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EquipoGrupoTrxRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.GrupoTrxRepositorioJpa;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioGruposTrxJpaAdaptador implements RepositorioGruposTrx {

    private final GrupoTrxRepositorioJpa grupoTrxRepositorioJpa;
    private final EquipoGrupoTrxRepositorioJpa equipoGrupoTrxRepositorioJpa;
    private final EquipoRepositorioJpa equipoRepositorioJpa;

    public RepositorioGruposTrxJpaAdaptador(
        GrupoTrxRepositorioJpa grupoTrxRepositorioJpa,
        EquipoGrupoTrxRepositorioJpa equipoGrupoTrxRepositorioJpa,
        EquipoRepositorioJpa equipoRepositorioJpa
    ) {
        this.grupoTrxRepositorioJpa = grupoTrxRepositorioJpa;
        this.equipoGrupoTrxRepositorioJpa = equipoGrupoTrxRepositorioJpa;
        this.equipoRepositorioJpa = equipoRepositorioJpa;
    }

    @Override
    public GrupoTrx crear(DatosGrupoTrx datos) {
        GrupoTrxEntidad entidad = new GrupoTrxEntidad(
            datos.codigo(),
            datos.nombre(),
            datos.descripcion(),
            datos.maximoEquipos(),
            datos.activo() == null || datos.activo()
        );
        return aDominio(grupoTrxRepositorioJpa.save(entidad));
    }

    @Override
    public GrupoTrx actualizar(UUID id, DatosGrupoTrx datos) {
        GrupoTrxEntidad entidad = obtenerEntidad(id);
        long equiposAsignados = equipoGrupoTrxRepositorioJpa.countByGrupoTrxId(id);
        if (datos.maximoEquipos() < equiposAsignados) {
            throw new IllegalArgumentException("El maximo no puede ser menor a los equipos ya asignados.");
        }
        entidad.actualizar(datos.codigo(), datos.nombre(), datos.descripcion(), datos.maximoEquipos(), datos.activo());
        return aDominio(grupoTrxRepositorioJpa.save(entidad));
    }

    @Override
    public GrupoTrx cambiarEstado(UUID id, EstadoGrupoTrx estado) {
        GrupoTrxEntidad entidad = obtenerEntidad(id);
        entidad.cambiarEstado(estado.name());
        return aDominio(grupoTrxRepositorioJpa.save(entidad));
    }

    @Override
    public Optional<DetalleGrupoTrx> buscarDetallePorId(UUID id) {
        return grupoTrxRepositorioJpa.findById(id).map(entidad -> {
            List<EquipoGrupoTrx> equipos = equipoGrupoTrxRepositorioJpa.findByGrupoTrxId(id)
                .stream()
                .sorted(Comparator.comparing(asignacion -> asignacion.getEquipo().getNombreEquipo()))
                .map(this::aEquipoGrupo)
                .toList();
            List<String> codigosFarmacia = equipos.stream()
                .map(EquipoGrupoTrx::codigoFarmacia)
                .distinct()
                .sorted()
                .toList();
            return new DetalleGrupoTrx(aDominio(entidad), equipos, codigosFarmacia);
        });
    }

    @Override
    public Optional<GrupoTrx> buscarPorCodigo(String codigo) {
        return grupoTrxRepositorioJpa.findByCodigo(codigo).map(this::aDominio);
    }

    @Override
    public List<GrupoTrx> listar() {
        return grupoTrxRepositorioJpa.findAll()
            .stream()
            .sorted(Comparator.comparing(GrupoTrxEntidad::getCodigo))
            .map(this::aDominio)
            .toList();
    }

    @Override
    public Pagina<GrupoTrx> listarPaginado(FiltroGruposTrx filtro) {
        String codigo = minusculaANulo(filtro.codigo());
        String estado = mayusculaANulo(filtro.estado());
        org.springframework.data.domain.Page<GrupoTrxEntidad> pagina = grupoTrxRepositorioJpa.buscarConFiltros(
            codigo != null,
            nuloAValor(codigo),
            estado != null,
            nuloAValor(estado),
            filtro.activo() != null,
            filtro.activo() != null && filtro.activo(),
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
    public GrupoTrx asignarEquipo(UUID idGrupo, UUID idEquipo) {
        GrupoTrxEntidad grupo = obtenerEntidad(idGrupo);
        EquipoEntidad equipo = equipoRepositorioJpa.findById(idEquipo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Equipo POS no encontrado: " + idEquipo));

        long asignados = equipoGrupoTrxRepositorioJpa.countByGrupoTrxId(idGrupo);
        Optional<EquipoGrupoTrxEntidad> asignacionExistente = equipoGrupoTrxRepositorioJpa.findByEquipoId(idEquipo);
        boolean yaPerteneceAlGrupo = asignacionExistente
            .map(asignacion -> asignacion.getGrupoTrx().getId().equals(idGrupo))
            .orElse(false);
        if (!yaPerteneceAlGrupo && asignados >= grupo.getMaximoEquipos()) {
            throw new IllegalArgumentException("El Grupo TRX ya alcanzo el maximo de " + grupo.getMaximoEquipos() + " equipos.");
        }

        EquipoGrupoTrxEntidad asignacion = asignacionExistente
            .orElseGet(() -> new EquipoGrupoTrxEntidad(equipo, grupo));
        asignacion.cambiarGrupo(grupo);
        equipoGrupoTrxRepositorioJpa.save(asignacion);
        return aDominio(grupo);
    }

    @Override
    public GrupoTrx quitarEquipo(UUID idGrupo, UUID idEquipo) {
        GrupoTrxEntidad grupo = obtenerEntidad(idGrupo);
        EquipoGrupoTrxEntidad asignacion = equipoGrupoTrxRepositorioJpa.findByEquipoId(idEquipo)
            .orElseThrow(() -> new RecursoNoEncontradoException("El Equipo POS no tiene Grupo TRX asignado: " + idEquipo));
        if (!asignacion.getGrupoTrx().getId().equals(idGrupo)) {
            throw new IllegalArgumentException("El Equipo POS no pertenece al Grupo TRX indicado.");
        }
        equipoGrupoTrxRepositorioJpa.delete(asignacion);
        return aDominio(grupo);
    }

    private GrupoTrxEntidad obtenerEntidad(UUID id) {
        return grupoTrxRepositorioJpa.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Grupo TRX no encontrado: " + id));
    }

    private GrupoTrx aDominio(GrupoTrxEntidad entidad) {
        UUID id = entidad.getId();
        long equiposAsignados = id == null ? 0 : equipoGrupoTrxRepositorioJpa.countByGrupoTrxId(id);
        long farmaciasInvolucradas = id == null ? 0 : equipoGrupoTrxRepositorioJpa.contarFarmaciasPorGrupo(id);
        return new GrupoTrx(
            id,
            entidad.getCodigo(),
            entidad.getNombre(),
            entidad.getDescripcion(),
            EstadoGrupoTrx.valueOf(entidad.getEstado()),
            entidad.getMaximoEquipos(),
            entidad.isActivo(),
            equiposAsignados,
            farmaciasInvolucradas,
            entidad.getCreadoEn(),
            entidad.getActualizadoEn()
        );
    }

    private EquipoGrupoTrx aEquipoGrupo(EquipoGrupoTrxEntidad entidad) {
        EquipoEntidad equipo = entidad.getEquipo();
        SucursalEntidad farmacia = equipo.getSucursal();
        return new EquipoGrupoTrx(
            equipo.getId(),
            equipo.getNombreEquipo(),
            farmacia.getId(),
            farmacia.getCodigo(),
            farmacia.getNombre(),
            equipo.getVersionPos(),
            equipo.getEstado(),
            equipo.getUltimoLatidoEn(),
            entidad.getAsignadoEn()
        );
    }

    private Sort aOrden(String orden) {
        String[] partes = orden == null ? new String[0] : orden.split(",", 2);
        String campo = partes.length > 0 ? partes[0] : "codigo";
        Sort.Direction direccion = partes.length > 1 && "desc".equalsIgnoreCase(partes[1])
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        return Sort.by(direccion, switch (campo) {
            case "nombre", "name" -> "nombre";
            case "estado", "status" -> "estado";
            case "activo", "active" -> "activo";
            case "updatedAt", "actualizadoEn" -> "actualizadoEn";
            default -> "codigo";
        });
    }

    private String minusculaANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase(Locale.ROOT);
    }

    private String mayusculaANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toUpperCase(Locale.ROOT);
    }

    private String nuloAValor(String valor) {
        return valor == null ? "" : valor;
    }
}
