package com.farmamia.posupdate.infraestructura.persistencia.entidad;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "update_events")
public class EventoActualizacionEntidad {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deployment_target_id")
    private ObjetivoDespliegueEntidad objetivoDespliegue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deployment_id")
    private DespliegueEntidad despliegue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private EquipoEntidad equipo;

    @Column(name = "event_type", nullable = false, length = 60)
    private String tipoEvento;

    @Column(name = "event_message")
    private String mensajeEvento;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Column(name = "old_version", length = 40)
    private String versionAnterior;

    @Column(name = "new_version", length = 40)
    private String versionNueva;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private String metadatosJson = "{}";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected EventoActualizacionEntidad() {
    }

    public EventoActualizacionEntidad(
        ObjetivoDespliegueEntidad objetivoDespliegue,
        EquipoEntidad equipo,
        String tipoEvento,
        String idempotencyKey,
        String mensajeEvento,
        String versionAnterior,
        String versionNueva,
        String metadatosJson
    ) {
        this.objetivoDespliegue = objetivoDespliegue;
        this.despliegue = objetivoDespliegue == null ? null : objetivoDespliegue.getDespliegue();
        this.equipo = equipo;
        this.tipoEvento = tipoEvento;
        this.idempotencyKey = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey;
        this.mensajeEvento = mensajeEvento;
        this.versionAnterior = versionAnterior;
        this.versionNueva = versionNueva;
        this.metadatosJson = metadatosJson == null || metadatosJson.isBlank() ? "{}" : metadatosJson;
    }

    public UUID getId() {
        return id;
    }

    public ObjetivoDespliegueEntidad getObjetivoDespliegue() {
        return objetivoDespliegue;
    }

    public DespliegueEntidad getDespliegue() {
        return despliegue;
    }

    public EquipoEntidad getEquipo() {
        return equipo;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public String getMensajeEvento() {
        return mensajeEvento;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getVersionAnterior() {
        return versionAnterior;
    }

    public String getVersionNueva() {
        return versionNueva;
    }

    public String getMetadatosJson() {
        return metadatosJson;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }
}
