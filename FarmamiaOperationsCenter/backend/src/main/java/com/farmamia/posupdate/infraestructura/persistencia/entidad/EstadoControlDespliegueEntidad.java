package com.farmamia.posupdate.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "deployment_control_state")
public class EstadoControlDespliegueEntidad {

    @Id
    @Column(name = "deployment_id")
    private UUID idDespliegue;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "deployment_id")
    private DespliegueEntidad despliegue;

    @Column(name = "status", nullable = false, length = 40)
    private String estado = "READY";

    @Column(name = "auto_pause_enabled", nullable = false)
    private boolean pausaAutomaticaHabilitada = true;

    @Column(name = "max_failure_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeMaximoFallo = BigDecimal.TEN;

    @Column(name = "retry_limit", nullable = false)
    private int limiteReintentos = 2;

    @Column(name = "next_wave_number", nullable = false)
    private int siguienteNumeroOleada = 1;

    @Column(name = "paused_reason")
    private String motivoPausa;

    @Column(name = "last_evaluated_at")
    private OffsetDateTime evaluadoEn;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime actualizadoEn;

    protected EstadoControlDespliegueEntidad() {
    }

    public EstadoControlDespliegueEntidad(
        DespliegueEntidad despliegue,
        BigDecimal porcentajeMaximoFallo,
        boolean pausaAutomaticaHabilitada,
        int limiteReintentos
    ) {
        this.despliegue = despliegue;
        this.porcentajeMaximoFallo = porcentajeMaximoFallo;
        this.pausaAutomaticaHabilitada = pausaAutomaticaHabilitada;
        this.limiteReintentos = limiteReintentos;
    }

    public UUID getIdDespliegue() {
        return idDespliegue;
    }

    public String getEstado() {
        return estado;
    }

    public boolean isPausaAutomaticaHabilitada() {
        return pausaAutomaticaHabilitada;
    }

    public BigDecimal getPorcentajeMaximoFallo() {
        return porcentajeMaximoFallo;
    }

    public int getLimiteReintentos() {
        return limiteReintentos;
    }

    public int getSiguienteNumeroOleada() {
        return siguienteNumeroOleada;
    }

    public String getMotivoPausa() {
        return motivoPausa;
    }

    public OffsetDateTime getEvaluadoEn() {
        return evaluadoEn;
    }

    public void marcarEvaluado() {
        evaluadoEn = OffsetDateTime.now();
    }

    public void correr(int siguienteNumeroOleada) {
        estado = "RUNNING";
        motivoPausa = null;
        this.siguienteNumeroOleada = siguienteNumeroOleada;
    }

    public void pausar(String motivo) {
        estado = "PAUSED";
        motivoPausa = motivo;
        marcarEvaluado();
    }

    public void fallar(String motivo) {
        estado = "FAILED";
        motivoPausa = motivo;
        marcarEvaluado();
    }

    public void completar() {
        estado = "COMPLETED";
        motivoPausa = null;
        marcarEvaluado();
    }
}
