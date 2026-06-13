package com.farmamia.operations.infraestructura.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "grupos_trx")
public class GrupoTrxEntidad {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 160)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "estado", nullable = false, length = 40)
    private String estado = "ACTIVO";

    @Column(name = "maximo_equipos", nullable = false)
    private int maximoEquipos = 100;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime actualizadoEn;

    protected GrupoTrxEntidad() {
    }

    public GrupoTrxEntidad(String codigo, String nombre, String descripcion, int maximoEquipos, boolean activo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.maximoEquipos = maximoEquipos;
        this.activo = activo;
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
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

    public int getMaximoEquipos() {
        return maximoEquipos;
    }

    public boolean isActivo() {
        return activo;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public OffsetDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public void actualizar(String codigo, String nombre, String descripcion, int maximoEquipos, Boolean activo) {
        if (codigo != null) {
            this.codigo = codigo;
        }
        if (nombre != null) {
            this.nombre = nombre;
        }
        this.descripcion = descripcion;
        this.maximoEquipos = maximoEquipos;
        if (activo != null) {
            this.activo = activo;
        }
    }

    public void cambiarEstado(String estado) {
        this.estado = estado;
        if ("RETIRADO".equals(estado)) {
            this.activo = false;
        }
    }
}
