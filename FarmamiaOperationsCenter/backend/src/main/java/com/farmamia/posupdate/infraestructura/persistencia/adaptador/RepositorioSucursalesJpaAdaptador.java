package com.farmamia.posupdate.infraestructura.persistencia.adaptador;

import com.farmamia.posupdate.dominio.modelo.FiltroSucursales;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.dominio.modelo.Sucursal;
import com.farmamia.posupdate.dominio.modelo.SucursalSugerida;
import com.farmamia.posupdate.dominio.puerto.RepositorioSucursales;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.GrupoTrxEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.SucursalEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.SucursalRepositorioJpa;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioSucursalesJpaAdaptador implements RepositorioSucursales {

    private final SucursalRepositorioJpa sucursalRepositorioJpa;

    public RepositorioSucursalesJpaAdaptador(SucursalRepositorioJpa sucursalRepositorioJpa) {
        this.sucursalRepositorioJpa = sucursalRepositorioJpa;
    }

    @Override
    public UUID obtenerOCrearPorCodigo(String codigoSucursal) {
        return sucursalRepositorioJpa.findByCodigo(codigoSucursal)
            .orElseGet(() -> sucursalRepositorioJpa.save(new SucursalEntidad(codigoSucursal, codigoSucursal)))
            .getId();
    }

    @Override
    public Optional<Sucursal> buscarPorCodigo(String codigo) {
        return sucursalRepositorioJpa.findByCodigo(codigo).map(this::aDominio);
    }

    @Override
    public Optional<SucursalSugerida> buscarSugeridaPorCodigo(String codigo) {
        return sucursalRepositorioJpa.findByCodigo(codigo).map(this::aSugerida);
    }

    @Override
    public long contarPorIds(Set<UUID> idsSucursales) {
        return idsSucursales.isEmpty() ? 0 : sucursalRepositorioJpa.countByIdIn(idsSucursales);
    }

    @Override
    public List<Sucursal> listar() {
        return sucursalRepositorioJpa.findAll()
            .stream()
            .sorted(Comparator.comparing(SucursalEntidad::getCodigo))
            .map(this::aDominio)
            .toList();
    }

    @Override
    public Pagina<Sucursal> listarPaginado(FiltroSucursales filtro) {
        String q = minusculaANulo(filtro.q());
        String codigo = minusculaANulo(filtro.codigo());
        String ciudad = minusculaANulo(filtro.ciudad());
        String zona = minusculaANulo(filtro.zona());
        org.springframework.data.domain.Page<SucursalEntidad> pagina = sucursalRepositorioJpa.buscarConFiltros(
            q != null,
            nuloAValor(q),
            codigo != null,
            nuloAValor(codigo),
            ciudad != null,
            nuloAValor(ciudad),
            zona != null,
            nuloAValor(zona),
            filtro.deTurno() != null,
            filtro.deTurno() != null && filtro.deTurno(),
            filtro.activa() != null,
            filtro.activa() != null && filtro.activa(),
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

    private Sucursal aDominio(SucursalEntidad entidad) {
        return new Sucursal(
            entidad.getId(),
            entidad.getCodigo(),
            entidad.getNombre(),
            entidad.getCiudad(),
            entidad.getZona(),
            entidad.getDireccion(),
            entidad.isDeTurno(),
            entidad.isActiva(),
            entidad.getCreadoEn(),
            entidad.getActualizadoEn()
        );
    }

    private SucursalSugerida aSugerida(SucursalEntidad entidad) {
        GrupoTrxEntidad grupoTrx = entidad.getGrupoTrx();
        return new SucursalSugerida(
            entidad.getId(),
            entidad.getCodigo(),
            entidad.getNombre(),
            grupoTrx == null ? null : grupoTrx.getCodigo()
        );
    }

    private Sort aOrden(String orden) {
        String[] partes = orden == null ? new String[0] : orden.split(",", 2);
        String campo = partes.length > 0 ? partes[0] : "codigo";
        Sort.Direction direccion = partes.length > 1 && "desc".equalsIgnoreCase(partes[1])
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        return Sort.by(direccion, switch (campo) {
            case "code", "codigo" -> "codigo";
            case "name", "nombre" -> "nombre";
            case "city", "ciudad" -> "ciudad";
            case "zone", "zona" -> "zona";
            case "onDuty", "deTurno" -> "deTurno";
            case "active", "activa" -> "activa";
            case "createdAt", "creadoEn" -> "creadoEn";
            case "updatedAt", "actualizadoEn" -> "actualizadoEn";
            default -> "codigo";
        });
    }

    private String minusculaANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase(Locale.ROOT);
    }

    private String nuloAValor(String valor) {
        return valor == null ? "" : valor;
    }
}
