package com.farmamia.posupdate.dominio.puerto;

import com.farmamia.posupdate.dominio.modelo.ArchivoPaqueteDescarga;
import com.farmamia.posupdate.dominio.modelo.ArchivoPaqueteGuardado;
import java.io.InputStream;

public interface AlmacenamientoPaquetes {

    ArchivoPaqueteGuardado guardar(String version, String nombreArchivoOriginal, InputStream contenido);

    ArchivoPaqueteDescarga descargar(String rutaAlmacenamiento, String nombreArchivo);
}
