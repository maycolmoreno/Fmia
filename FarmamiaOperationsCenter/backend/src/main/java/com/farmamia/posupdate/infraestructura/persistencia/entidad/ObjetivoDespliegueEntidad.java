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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wave_id")
    private OleadaDespliegueEntidad oleada;

    @Column(name = "target_group", length = 40)
    private String grupoObjetivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_trx_id")
    private GrupoTrxEntidad grupoTrx;

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

    @Column(name = "last_instruction_issued_at")
    private OffsetDateTime ultimaInstruccionEmitidaEn;

    @Column(name = "instruction_lease_until")
    private OffsetDateTime leaseInstruccionHasta;

    @Column(name = "next_retry_at")
    private OffsetDateTime siguienteReintentoEn;

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
        this.estado = "PENDING";
    }

    public UUID getId() {
        return id;
    }

    public DespliegueEntidad getDespliegue() {
        return despliegue;
    }

    public EquipoEntidad getEquipo() {
        return equipo;
    }

    public OleadaDespliegueEntidad getOleada() {
        return oleada;
    }

    public String getGrupoObjetivo() {
        return grupoObjetivo;
    }

    public GrupoTrxEntidad getGrupoTrx() {
        return grupoTrx;
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

    public OffsetDateTime getLeaseInstruccionHasta() {
        return leaseInstruccionHasta;
    }

    public Integer getCantidadIntentos() {
        return cantidadIntentos;
    }

    public OffsetDateTime getSiguienteReintentoEn() {
        return siguienteReintentoEn;
    }

    public OffsetDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public boolean estaAutorizado() {
        return "AUTHORIZED".equals(estado);
    }

    public void asignarOleada(OleadaDespliegueEntidad oleada) {
        this.oleada = oleada;
    }

    public void asignarGrupoTrx(GrupoTrxEntidad grupoTrx) {
        this.grupoTrx = grupoTrx;
        if (grupoTrx != null) {
            this.grupoObjetivo = grupoTrx.getCodigo();
        }
    }

    public void autorizarDesdeOleada() {
        if (!"PENDING".equals(estado) && !"AUTHORIZED".equals(estado)) {
            return;
        }
        this.estado = "AUTHORIZED";
        this.autorizadoEn = autorizadoEn == null ? OffsetDateTime.now() : autorizadoEn;
    }

    public boolean tieneLeaseActivo(OffsetDateTime ahora) {
        return leaseInstruccionHasta != null && leaseInstruccionHasta.isAfter(ahora);
    }

    public void registrarLeaseInstruccion(OffsetDateTime ahora, int minutosLease) {
        this.ultimaInstruccionEmitidaEn = ahora;
        this.leaseInstruccionHasta = ahora.plusMinutes(minutosLease);
    }

    public void liberarLeaseInstruccion() {
        this.leaseInstruccionHasta = null;
    }

    public boolean esFalloReintentable() {
        return "FAILED".equals(estado) || "ROLLBACK_FAILED".equals(estado);
    }

    public boolean reintentoDisponible(OffsetDateTime ahora, int limiteReintentos) {
        return esFalloReintentable()
            && cantidadIntentos <= limiteReintentos
            && (siguienteReintentoEn == null || !siguienteReintentoEn.isAfter(ahora));
    }

    public boolean reintentosAgotados(int limiteReintentos) {
        return esFalloReintentable() && cantidadIntentos > limiteReintentos;
    }

    public void autorizarReintento() {
        if (!esFalloReintentable()) {
            return;
        }
        this.estado = "AUTHORIZED";
        this.autorizadoEn = OffsetDateTime.now();
        this.siguienteReintentoEn = null;
    }

    public void registrarResultado(String estado, String versionAnterior, String versionNueva, String mensajeError) {
        this.estado = estado;
        this.versionAnterior = blancoANulo(versionAnterior);
        this.versionNueva = blancoANulo(versionNueva);
        this.ultimoError = blancoANulo(mensajeError);
        this.completadoEn = esEstadoFinal(estado) ? OffsetDateTime.now() : this.completadoEn;
        if (esEstadoFinal(estado)) {
            liberarLeaseInstruccion();
        }
        if ("FAILED".equals(estado) || "ROLLBACK_FAILED".equals(estado)) {
            cantidadIntentos = cantidadIntentos == null ? 1 : cantidadIntentos + 1;
            siguienteReintentoEn = OffsetDateTime.now().plusMinutes(minutosBackoff(cantidadIntentos));
        } else if ("COMPLETED".equals(estado) || "ROLLBACK_COMPLETED".equals(estado)) {
            siguienteReintentoEn = null;
        }
    }

    private int minutosBackoff(int intento) {
        return Math.min(60, (int) Math.pow(2, Math.max(0, intento - 1)) * 5);
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
