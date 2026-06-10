package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.modelo.PaquetePos;
import com.farmamia.operations.dominio.puerto.RepositorioPaquetesPos;
import com.farmamia.operations.infraestructura.persistencia.entidad.PaquetePosEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.PaquetePosRepositorioJpa;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

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
}
