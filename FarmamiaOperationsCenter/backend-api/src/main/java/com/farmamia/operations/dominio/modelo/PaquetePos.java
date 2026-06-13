package com.farmamia.operations.dominio.modelo;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PaquetePos {

    private final UUID id;
    private final String version;
    private final String nombreArchivo;
    private final String rutaAlmacenamiento;
    private final String checksumSha256;
    private final Long tamanoBytes;
    private final FirmaPaquetePos firma;
    private String estado;
    private final OffsetDateTime cargadoEn;
    private OffsetDateTime aprobadoEn;

    public PaquetePos(
        UUID id,
        String version,
        String nombreArchivo,
        String rutaAlmacenamiento,
        String checksumSha256,
        Long tamanoBytes,
        FirmaPaquetePos firma,
        String estado,
        OffsetDateTime cargadoEn,
        OffsetDateTime aprobadoEn
    ) {
        this.id = id;
        this.version = version;
        this.nombreArchivo = nombreArchivo;
        this.rutaAlmacenamiento = rutaAlmacenamiento;
        this.checksumSha256 = checksumSha256;
        this.tamanoBytes = tamanoBytes;
        this.firma = firma;
        this.estado = estado;
        this.cargadoEn = cargadoEn;
        this.aprobadoEn = aprobadoEn;
    }

    public static PaquetePos validado(
        String version,
        String nombreArchivo,
        String rutaAlmacenamiento,
        String checksumSha256,
        Long tamanoBytes,
        FirmaPaquetePos firma
    ) {
        return new PaquetePos(
            null,
            version,
            nombreArchivo,
            rutaAlmacenamiento,
            checksumSha256,
            tamanoBytes,
            firma,
            "VALIDATED",
            null,
            null
        );
    }

    public UUID getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getRutaAlmacenamiento() {
        return rutaAlmacenamiento;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public Long getTamanoBytes() {
        return tamanoBytes;
    }

    public FirmaPaquetePos getFirma() {
        return firma;
    }

    public String getEstado() {
        return estado;
    }

    public OffsetDateTime getCargadoEn() {
        return cargadoEn;
    }

    public OffsetDateTime getAprobadoEn() {
        return aprobadoEn;
    }

    public boolean estaAprobado() {
        return "APPROVED".equals(estado);
    }
}
