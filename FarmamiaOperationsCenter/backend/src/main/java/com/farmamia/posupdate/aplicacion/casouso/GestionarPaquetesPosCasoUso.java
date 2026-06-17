package com.farmamia.posupdate.aplicacion.casouso;

import com.farmamia.posupdate.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.posupdate.dominio.modelo.ArchivoPaqueteDescarga;
import com.farmamia.posupdate.dominio.modelo.ArchivoPaqueteGuardado;
import com.farmamia.posupdate.dominio.modelo.FiltroPaquetesPos;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.dominio.modelo.PaquetePos;
import com.farmamia.posupdate.dominio.puerto.AlmacenamientoPaquetes;
import com.farmamia.posupdate.dominio.puerto.FirmadorPaquetesPos;
import com.farmamia.posupdate.dominio.puerto.RepositorioPaquetesPos;
import jakarta.transaction.Transactional;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GestionarPaquetesPosCasoUso {

    private final RepositorioPaquetesPos repositorioPaquetesPos;
    private final AlmacenamientoPaquetes almacenamientoPaquetes;
    private final FirmadorPaquetesPos firmadorPaquetesPos;

    public GestionarPaquetesPosCasoUso(
        RepositorioPaquetesPos repositorioPaquetesPos,
        AlmacenamientoPaquetes almacenamientoPaquetes,
        FirmadorPaquetesPos firmadorPaquetesPos
    ) {
        this.repositorioPaquetesPos = repositorioPaquetesPos;
        this.almacenamientoPaquetes = almacenamientoPaquetes;
        this.firmadorPaquetesPos = firmadorPaquetesPos;
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
            archivoGuardado.tamanoBytes(),
            firmadorPaquetesPos.firmarChecksum(archivoGuardado.checksumSha256())
        );

        return repositorioPaquetesPos.guardarNuevo(paquete);
    }

    public List<PaquetePos> listar() {
        return repositorioPaquetesPos.listar();
    }

    public Pagina<PaquetePos> listarPaginado(FiltroPaquetesPos filtro) {
        return repositorioPaquetesPos.listarPaginado(new FiltroPaquetesPos(
            blancoANulo(filtro.q()),
            blancoANulo(filtro.estado()),
            blancoANulo(filtro.version()),
            filtro.cargadoDesde(),
            filtro.cargadoHasta(),
            Math.max(0, filtro.pagina()),
            Math.max(1, Math.min(filtro.tamano(), 200)),
            blancoANulo(filtro.orden()) == null ? "cargadoEn,desc" : filtro.orden()
        ));
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

    private String blancoANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
