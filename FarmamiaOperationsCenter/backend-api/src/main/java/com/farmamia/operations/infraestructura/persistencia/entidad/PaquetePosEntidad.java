package com.farmamia.operations.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "pos_packages")
public class PaquetePosEntidad {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "version", nullable = false, unique = true, length = 40)
    private String version;

    @Column(name = "file_name", nullable = false, length = 260)
    private String nombreArchivo;

    @Column(name = "storage_path", nullable = false)
    private String rutaAlmacenamiento;

    @Column(name = "sha256_checksum", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "size_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "status", nullable = false, length = 40)
    private String estado = "UPLOADED";

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private OffsetDateTime cargadoEn;

    @Column(name = "approved_at")
    private OffsetDateTime aprobadoEn;

    @Column(name = "retired_at")
    private OffsetDateTime retiradoEn;

    @Column(name = "notes")
    private String notas;

    protected PaquetePosEntidad() {
    }

    public PaquetePosEntidad(
        String version,
        String nombreArchivo,
        String rutaAlmacenamiento,
        String checksumSha256,
        Long tamanoBytes
    ) {
        this.version = version;
        this.nombreArchivo = nombreArchivo;
        this.rutaAlmacenamiento = rutaAlmacenamiento;
        this.checksumSha256 = checksumSha256;
        this.tamanoBytes = tamanoBytes;
        this.estado = "VALIDATED";
    }

    public UUID getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getRutaAlmacenamiento() {
        return rutaAlmacenamiento;
    }

    public Long getTamanoBytes() {
        return tamanoBytes;
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

    public void aprobar() {
        this.estado = "APPROVED";
        this.aprobadoEn = OffsetDateTime.now();
    }

    public void retirar() {
        this.estado = "RETIRED";
        this.retiradoEn = OffsetDateTime.now();
    }
}
