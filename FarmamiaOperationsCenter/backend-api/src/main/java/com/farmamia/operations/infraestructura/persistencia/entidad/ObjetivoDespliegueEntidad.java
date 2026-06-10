package com.farmamia.operations.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "deployment_targets")
public class ObjetivoDespliegueEntidad {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deployment_id", nullable = false)
    private DespliegueEntidad despliegue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private EquipoEntidad equipo;

    @Column(name = "target_group", length = 40)
    private String grupoObjetivo;

    @Column(name = "is_pilot", nullable = false)
    private boolean piloto;

    @Column(name = "status", nullable = false, length = 40)
    private String estado = "PENDING";

    @Column(name = "old_version", length = 40)
    private String versionAnterior;

    @Column(name = "new_version", length = 40)
    private String versionNueva;

    @Column(name = "attempt_count", nullable = false)
    private Integer cantidadIntentos = 0;

    @Column(name = "last_error")
    private String ultimoError;

    @Column(name = "authorized_at")
    private OffsetDateTime autorizadoEn;

    @Column(name = "started_at")
    private OffsetDateTime iniciadoEn;

    @Column(name = "completed_at")
    private OffsetDateTime completadoEn;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime actualizadoEn;

    protected ObjetivoDespliegueEntidad() {
    }

    public ObjetivoDespliegueEntidad(
        DespliegueEntidad despliegue,
        EquipoEntidad equipo,
        String grupoObjetivo,
        boolean piloto,
        String versionNueva
    ) {
        this.despliegue = despliegue;
        this.equipo = equipo;
        this.grupoObjetivo = grupoObjetivo;
        this.piloto = piloto;
        this.versionNueva = versionNueva;
        this.estado = "AUTHORIZED";
        this.autorizadoEn = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public DespliegueEntidad getDespliegue() {
        return despliegue;
    }

    public String getGrupoObjetivo() {
        return grupoObjetivo;
    }

    public boolean isPiloto() {
        return piloto;
    }

    public String getEstado() {
        return estado;
    }

    public String getVersionAnterior() {
        return versionAnterior;
    }

    public String getVersionNueva() {
        return versionNueva;
    }

    public String getUltimoError() {
        return ultimoError;
    }

    public OffsetDateTime getAutorizadoEn() {
        return autorizadoEn;
    }

    public OffsetDateTime getIniciadoEn() {
        return iniciadoEn;
    }

    public OffsetDateTime getCompletadoEn() {
        return completadoEn;
    }

    public OffsetDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public boolean estaAutorizado() {
        return "AUTHORIZED".equals(estado);
    }

    public void registrarResultado(String estado, String versionAnterior, String versionNueva, String mensajeError) {
        this.estado = estado;
        this.versionAnterior = blancoANulo(versionAnterior);
        this.versionNueva = blancoANulo(versionNueva);
        this.ultimoError = blancoANulo(mensajeError);
        this.completadoEn = esEstadoFinal(estado) ? OffsetDateTime.now() : this.completadoEn;
    }

    private boolean esEstadoFinal(String estado) {
        return "COMPLETED".equals(estado)
            || "FAILED".equals(estado)
            || "ROLLBACK_COMPLETED".equals(estado)
            || "ROLLBACK_FAILED".equals(estado)
            || "SKIPPED".equals(estado);
    }

    private String blancoANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
