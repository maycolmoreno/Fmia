package com.farmamia.operations.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "deployment_waves")
public class OleadaDespliegueEntidad {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deployment_id", nullable = false)
    private DespliegueEntidad despliegue;

    @Column(name = "wave_number", nullable = false)
    private int numero;

    @Column(name = "name", nullable = false, length = 120)
    private String nombre;

    @Column(name = "target_group", length = 40)
    private String grupoObjetivo;

    @Column(name = "is_pilot", nullable = false)
    private boolean piloto;

    @Column(name = "status", nullable = false, length = 40)
    private String estado = "PLANNED";

    @Column(name = "max_failure_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeMaximoFallo = BigDecimal.TEN;

    @Column(name = "auto_pause_enabled", nullable = false)
    private boolean pausaAutomaticaHabilitada = true;

    @Column(name = "maintenance_window_start")
    private LocalTime ventanaInicio;

    @Column(name = "maintenance_window_end")
    private LocalTime ventanaFin;

    @Column(name = "planned_targets", nullable = false)
    private int objetivosPlanificados;

    @Column(name = "started_at")
    private OffsetDateTime iniciadoEn;

    @Column(name = "completed_at")
    private OffsetDateTime completadoEn;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime actualizadoEn;

    protected OleadaDespliegueEntidad() {
    }

    public OleadaDespliegueEntidad(
        DespliegueEntidad despliegue,
        int numero,
        String nombre,
        String grupoObjetivo,
        boolean piloto,
        BigDecimal porcentajeMaximoFallo,
        boolean pausaAutomaticaHabilitada,
        LocalTime ventanaInicio,
        LocalTime ventanaFin,
        int objetivosPlanificados
    ) {
        this.despliegue = despliegue;
        this.numero = numero;
        this.nombre = nombre;
        this.grupoObjetivo = grupoObjetivo;
        this.piloto = piloto;
        this.porcentajeMaximoFallo = porcentajeMaximoFallo;
        this.pausaAutomaticaHabilitada = pausaAutomaticaHabilitada;
        this.ventanaInicio = ventanaInicio;
        this.ventanaFin = ventanaFin;
        this.objetivosPlanificados = objetivosPlanificados;
    }

    public UUID getId() {
        return id;
    }

    public DespliegueEntidad getDespliegue() {
        return despliegue;
    }

    public int getNumero() {
        return numero;
    }

    public String getNombre() {
        return nombre;
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

    public BigDecimal getPorcentajeMaximoFallo() {
        return porcentajeMaximoFallo;
    }

    public boolean isPausaAutomaticaHabilitada() {
        return pausaAutomaticaHabilitada;
    }

    public LocalTime getVentanaInicio() {
        return ventanaInicio;
    }

    public LocalTime getVentanaFin() {
        return ventanaFin;
    }

    public int getObjetivosPlanificados() {
        return objetivosPlanificados;
    }

    public OffsetDateTime getIniciadoEn() {
        return iniciadoEn;
    }

    public OffsetDateTime getCompletadoEn() {
        return completadoEn;
    }

    public void iniciar() {
        if (!"PLANNED".equals(estado) && !"PAUSED".equals(estado)) {
            throw new IllegalArgumentException("Solo se puede iniciar una oleada planificada o pausada.");
        }
        estado = "RUNNING";
        iniciadoEn = iniciadoEn == null ? OffsetDateTime.now() : iniciadoEn;
    }

    public void pausar() {
        if (!"RUNNING".equals(estado) && !"PLANNED".equals(estado)) {
            throw new IllegalArgumentException("Solo se puede pausar una oleada planificada o en ejecucion.");
        }
        estado = "PAUSED";
    }

    public void completar() {
        estado = "COMPLETED";
        completadoEn = OffsetDateTime.now();
    }

    public void fallar() {
        estado = "FAILED";
        completadoEn = OffsetDateTime.now();
    }
}
