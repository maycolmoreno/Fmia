package com.farmamia.posupdate.infraestructura.persistencia.adaptador;

import com.farmamia.posupdate.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.posupdate.dominio.modelo.FiltroPaquetesPos;
import com.farmamia.posupdate.dominio.modelo.FirmaPaquetePos;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.dominio.modelo.PaquetePos;
import com.farmamia.posupdate.dominio.puerto.RepositorioPaquetesPos;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.PaquetePosEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.PaquetePosRepositorioJpa;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Repository
public class RepositorioPaquetesPosJpaAdaptador implements RepositorioPaquetesPos {

    private static final OffsetDateTime FECHA_NEUTRA = OffsetDateTime.parse("1970-01-01T00:00:00Z");

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
            paquete.getTamanoBytes(),
            paquete.getFirma() == null ? null : paquete.getFirma().firma(),
            paquete.getFirma() == null ? null : paquete.getFirma().algoritmo(),
            paquete.getFirma() == null ? null : paquete.getFirma().idClave(),
            paquete.getFirma() == null ? null : paquete.getFirma().clavePublicaPem(),
            paquete.getFirma() == null ? null : paquete.getFirma().firmadoEn(),
            paquete.getFirma() == null ? "UNSIGNED" : paquete.getFirma().estado()
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
        String q = minusculaANulo(filtro.q());
        String estado = minusculaANulo(filtro.estado());
        String version = minusculaANulo(filtro.version());
        org.springframework.data.domain.Page<PaquetePosEntidad> pagina = paquetePosRepositorioJpa.buscarConFiltros(
            q != null,
            nuloAValor(q),
            estado != null,
            nuloAValor(estado),
            version != null,
            nuloAValor(version),
            filtro.cargadoDesde() != null,
            filtro.cargadoDesde() == null ? FECHA_NEUTRA : filtro.cargadoDesde(),
            filtro.cargadoHasta() != null,
            filtro.cargadoHasta() == null ? FECHA_NEUTRA : filtro.cargadoHasta(),
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
            aFirma(entidad),
            entidad.getEstado(),
            entidad.getCargadoEn(),
            entidad.getAprobadoEn()
        );
    }

    private FirmaPaquetePos aFirma(PaquetePosEntidad entidad) {
        if (entidad.getFirma() == null || entidad.getFirma().isBlank()) {
            return null;
        }
        return new FirmaPaquetePos(
            entidad.getFirma(),
            entidad.getAlgoritmoFirma(),
            entidad.getIdClaveFirma(),
            entidad.getClavePublicaFirmaPem(),
            entidad.getFirmadoEn(),
            entidad.getEstadoFirma()
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
            case "uploadedAt", "cargadoEn" -> "cargadoEn";
            case "approvedAt", "aprobadoEn" -> "aprobadoEn";
            case "sizeBytes", "tamanoBytes" -> "tamanoBytes";
            default -> "cargadoEn";
        });
    }

    private String minusculaANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase(Locale.ROOT);
    }

    private String nuloAValor(String valor) {
        return valor == null ? "" : valor;
    }
}
