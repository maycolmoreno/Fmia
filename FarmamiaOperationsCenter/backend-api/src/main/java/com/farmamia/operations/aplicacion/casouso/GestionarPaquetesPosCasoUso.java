package com.farmamia.operations.aplicacion.casouso;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.modelo.ArchivoPaqueteDescarga;
import com.farmamia.operations.dominio.modelo.ArchivoPaqueteGuardado;
import com.farmamia.operations.dominio.modelo.PaquetePos;
import com.farmamia.operations.dominio.puerto.AlmacenamientoPaquetes;
import com.farmamia.operations.dominio.puerto.RepositorioPaquetesPos;
import jakarta.transaction.Transactional;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GestionarPaquetesPosCasoUso {

    private final RepositorioPaquetesPos repositorioPaquetesPos;
    private final AlmacenamientoPaquetes almacenamientoPaquetes;

    public GestionarPaquetesPosCasoUso(
        RepositorioPaquetesPos repositorioPaquetesPos,
        AlmacenamientoPaquetes almacenamientoPaquetes
    ) {
        this.repositorioPaquetesPos = repositorioPaquetesPos;
        this.almacenamientoPaquetes = almacenamientoPaquetes;
    }

    @Transactional
    public PaquetePos cargar(String version, String nombreArchivoOriginal, InputStream contenido) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("La version del paquete es obligatoria");
        }

        if (repositorioPaquetesPos.existeVersion(version)) {
            throw new IllegalArgumentException("Ya existe un paquete POS con version: " + version);
        }

        ArchivoPaqueteGuardado archivoGuardado = almacenamientoPaquetes.guardar(version, nombreArchivoOriginal, contenido);
        PaquetePos paquete = PaquetePos.validado(
            version,
            archivoGuardado.nombreArchivo(),
            archivoGuardado.rutaAlmacenamiento(),
            archivoGuardado.checksumSha256(),
            archivoGuardado.tamanoBytes()
        );

        return repositorioPaquetesPos.guardarNuevo(paquete);
    }

    public List<PaquetePos> listar() {
        return repositorioPaquetesPos.listar();
    }

    public PaquetePos obtener(UUID id) {
        return buscarPaquete(id);
    }

    @Transactional
    public PaquetePos aprobar(UUID id) {
        return repositorioPaquetesPos.aprobar(id);
    }

    @Transactional
    public PaquetePos retirar(UUID id) {
        return repositorioPaquetesPos.retirar(id);
    }

    public ArchivoPaqueteDescarga descargar(UUID id) {
        PaquetePos paquete = buscarPaquete(id);
        if (!paquete.estaAprobado()) {
            throw new IllegalArgumentException("El paquete POS no esta aprobado para descarga");
        }

        return almacenamientoPaquetes.descargar(paquete.getRutaAlmacenamiento(), paquete.getNombreArchivo());
    }

    private PaquetePos buscarPaquete(UUID id) {
        return repositorioPaquetesPos.buscarPorId(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Paquete POS no encontrado: " + id));
    }
}
