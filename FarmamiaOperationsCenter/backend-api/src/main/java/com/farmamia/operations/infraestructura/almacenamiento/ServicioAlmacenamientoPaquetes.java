package com.farmamia.operations.infraestructura.almacenamiento;

import com.farmamia.operations.dominio.modelo.ArchivoPaqueteDescarga;
import com.farmamia.operations.dominio.modelo.ArchivoPaqueteGuardado;
import com.farmamia.operations.dominio.puerto.AlmacenamientoPaquetes;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ServicioAlmacenamientoPaquetes implements AlmacenamientoPaquetes {

    private final Path rutaBase;

    public ServicioAlmacenamientoPaquetes(
        @Value("${farmamia.paquetes.ruta-almacenamiento}") String rutaAlmacenamiento
    ) {
        this.rutaBase = Path.of(rutaAlmacenamiento).toAbsolutePath().normalize();
    }

    @Override
    public ArchivoPaqueteGuardado guardar(String version, String nombreArchivoOriginal, InputStream contenido) {
        validarArchivoZip(nombreArchivoOriginal, contenido);

        try {
            Files.createDirectories(rutaBase);
            String nombreArchivo = limpiarNombreArchivo(version + "-" + nombreArchivoOriginal);
            Path destino = rutaBase.resolve(nombreArchivo).normalize();

            if (!destino.startsWith(rutaBase)) {
                throw new IllegalArgumentException("Ruta de paquete invalida");
            }

            Files.copy(contenido, destino);
            String checksum = calcularSha256(destino);

            return new ArchivoPaqueteGuardado(
                nombreArchivo,
                destino.toString(),
                checksum,
                Files.size(destino)
            );
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar el paquete POS", ex);
        }
    }

    @Override
    public ArchivoPaqueteDescarga descargar(String rutaAlmacenamiento, String nombreArchivo) {
        try {
            Path ruta = Path.of(rutaAlmacenamiento).toAbsolutePath().normalize();
            if (!Files.exists(ruta) || !Files.isReadable(ruta)) {
                throw new IllegalStateException("El paquete no existe o no es legible");
            }
            return new ArchivoPaqueteDescarga(nombreArchivo, Files.newInputStream(ruta));
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el paquete POS", ex);
        }
    }

    private void validarArchivoZip(String nombreArchivoOriginal, InputStream contenido) {
        if (contenido == null) {
            throw new IllegalArgumentException("El archivo ZIP es obligatorio");
        }

        if (nombreArchivoOriginal == null || !nombreArchivoOriginal.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("El paquete POS debe ser un archivo .zip");
        }
    }

    private String limpiarNombreArchivo(String nombreArchivo) {
        return nombreArchivo.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String calcularSha256(Path archivo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(archivo);
                 DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                digestInput.transferTo(OutputStreamNulo.INSTANCE);
            }
            return aHexadecimal(digest.digest());
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No se pudo calcular SHA-256 del paquete", ex);
        }
    }

    private String aHexadecimal(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte valor : bytes) {
            builder.append(String.format("%02x", valor));
        }
        return builder.toString();
    }
}
