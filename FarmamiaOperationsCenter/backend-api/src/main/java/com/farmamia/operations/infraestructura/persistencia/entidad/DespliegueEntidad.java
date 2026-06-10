package com.farmamia.operations.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "deployments")
public class DespliegueEntidad {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private PaquetePosEntidad paquete;

    @Column(name = "name", nullable = false, length = 180)
    private String nombre;

    @Column(name = "description")
    private String descripcion;

    @Column(name = "status", nullable = false, length = 40)
    private String estado = "DRAFT";

    @Column(name = "pilot_required", nullable = false)
    private boolean pilotoRequerido = true;

    @Column(name = "scheduled_at")
    private OffsetDateTime programadoEn;

    @Column(name = "official_update_time", nullable = false)
    private LocalTime horaOficialActualizacion = LocalTime.of(23, 55);

    @Column(name = "force_update_time", nullable = false)
    private LocalTime horaForzadaActualizacion = LocalTime.of(1, 0);

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "approved_at")
    private OffsetDateTime aprobadoEn;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime actualizadoEn;

    protected DespliegueEntidad() {
    }

    public DespliegueEntidad(
        PaquetePosEntidad paquete,
        String nombre,
        String descripcion,
        OffsetDateTime programadoEn
    ) {
        this.paquete = paquete;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.programadoEn = programadoEn;
        this.estado = programadoEn == null ? "RUNNING" : "SCHEDULED";
    }

    public UUID getId() {
        return id;
    }

    public PaquetePosEntidad getPaquete() {
        return paquete;
    }

    public LocalTime getHoraOficialActualizacion() {
        return horaOficialActualizacion;
    }

    public LocalTime getHoraForzadaActualizacion() {
        return horaForzadaActualizacion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public OffsetDateTime getProgramadoEn() {
        return programadoEn;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public boolean puedeEntregarInstrucciones() {
        return "SCHEDULED".equals(estado)
            || "APPROVED".equals(estado)
            || "PILOT_RUNNING".equals(estado)
            || "RUNNING".equals(estado);
    }

    public void programar(OffsetDateTime programadoEn) {
        this.programadoEn = programadoEn;
        this.estado = "SCHEDULED";
    }

    public void pausar() {
        this.estado = "PAUSED";
    }

    public void reanudar() {
        this.estado = "RUNNING";
    }

    public void cancelar() {
        this.estado = "CANCELLED";
    }
}
