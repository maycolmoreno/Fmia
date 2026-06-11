package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.modelo.FiltroPaquetesPos;
import com.farmamia.operations.dominio.modelo.Pagina;
import com.farmamia.operations.dominio.modelo.PaquetePos;
import com.farmamia.operations.dominio.puerto.RepositorioPaquetesPos;
import com.farmamia.operations.infraestructura.persistencia.entidad.PaquetePosEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.PaquetePosRepositorioJpa;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Repository
public class RepositorioPaquetesPosJpaAdaptador implements RepositorioPaquetesPos {

    private final PaquetePosRepositorioJpa paquetePosRepositorioJpa;

    public RepositorioPaquetesPosJpaAdaptador(PaquetePosRepositorioJpa paquetePosRepositorioJpa) {
        this.paquetePosRepositorioJpa = paquetePosRepositorioJpa;
    }

    @Override
    public boolean existeVersion(String version) {
        return paquetePosRepositorioJpa.findByVersion(version).isPresent();
    }

    @Override
    public PaquetePos guardarNuevo(PaquetePos paquete) {
        PaquetePosEntidad entidad = new PaquetePosEntidad(
            paquete.getVersion(),
            paquete.getNombreArchivo(),
            paquete.getRutaAlmacenamiento(),
            paquete.getChecksumSha256(),
            paquete.getTamanoBytes()
        );

        return aDominio(paquetePosRepositorioJpa.save(entidad));
    }

    @Override
    public List<PaquetePos> listar() {
        return paquetePosRepositorioJpa.findAll()
            .stream()
            .map(this::aDominio)
            .toList();
    }

    @Override
    public Pagina<PaquetePos> listarPaginado(FiltroPaquetesPos filtro) {
        org.springframework.data.domain.Page<PaquetePosEntidad> pagina = paquetePosRepositorioJpa.buscarConFiltros(
            minusculaANulo(filtro.q()),
            minusculaANulo(filtro.estado()),
            minusculaANulo(filtro.version()),
            filtro.cargadoDesde(),
            filtro.cargadoHasta(),
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
    public Optional<PaquetePos> buscarPorId(UUID id) {
        return paquetePosRepositorioJpa.findById(id).map(this::aDominio);
    }

    @Override
    public PaquetePos aprobar(UUID id) {
        PaquetePosEntidad entidad = buscarEntidad(id);
        entidad.aprobar();
        return aDominio(entidad);
    }

    @Override
    public PaquetePos retirar(UUID id) {
        PaquetePosEntidad entidad = buscarEntidad(id);
        entidad.retirar();
        return aDominio(entidad);
    }

    private PaquetePosEntidad buscarEntidad(UUID id) {
        return paquetePosRepositorioJpa.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Paquete POS no encontrado: " + id));
    }

    private PaquetePos aDominio(PaquetePosEntidad entidad) {
        return new PaquetePos(
            entidad.getId(),
            entidad.getVersion(),
            entidad.getNombreArchivo(),
            entidad.getRutaAlmacenamiento(),
            entidad.getChecksumSha256(),
            entidad.getTamanoBytes(),
            entidad.getEstado(),
            entidad.getCargadoEn(),
            entidad.getAprobadoEn()
        );
    }

    private Sort aOrden(String orden) {
        String[] partes = orden == null ? new String[0] : orden.split(",", 2);
        String campo = partes.length > 0 ? partes[0] : "cargadoEn";
        Sort.Direction direccion = partes.length > 1 && "asc".equalsIgnoreCase(partes[1])
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        return Sort.by(direccion, switch (campo) {
            case "version" -> "version";
            case "fileName", "nombreArchivo" -> "nombreArchivo";
            case "status", "estado" -> "estado";
            case "approvedAt", "aprobadoEn" -> "aprobadoEn";
            case "sizeBytes", "tamanoBytes" -> "tamanoBytes";
            default -> "cargadoEn";
        });
    }

    private String minusculaANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase(Locale.ROOT);
    }
}
