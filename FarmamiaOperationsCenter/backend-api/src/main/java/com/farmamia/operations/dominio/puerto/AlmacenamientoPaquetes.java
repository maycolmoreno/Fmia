package com.farmamia.operations.dominio.puerto;

import com.farmamia.operations.dominio.modelo.ArchivoPaqueteDescarga;
import com.farmamia.operations.dominio.modelo.ArchivoPaqueteGuardado;
import java.io.InputStream;

public interface AlmacenamientoPaquetes {

    ArchivoPaqueteGuardado guardar(String version, String nombreArchivoOriginal, InputStream contenido);

    ArchivoPaqueteDescarga descargar(String rutaAlmacenamiento, String nombreArchivo);
}
