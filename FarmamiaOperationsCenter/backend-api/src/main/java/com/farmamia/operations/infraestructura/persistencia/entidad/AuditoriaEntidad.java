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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_logs")
public class AuditoriaEntidad {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private UsuarioAppEntidad usuarioActor;

    @Column(name = "action", nullable = false, length = 100)
    private String accion;

    @Column(name = "entity_type", nullable = false, length = 80)
    private String tipoEntidad;

    @Column(name = "entity_id")
    private UUID idEntidad;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "jsonb")
    private String valoresAnterioresJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "jsonb")
    private String valoresNuevosJson;

    @Column(name = "ip_address", length = 45)
    private String direccionIp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected AuditoriaEntidad() {
    }

    public AuditoriaEntidad(
        UsuarioAppEntidad usuarioActor,
        String accion,
        String tipoEntidad,
        UUID idEntidad,
        String valoresAnterioresJson,
        String valoresNuevosJson,
        String direccionIp
    ) {
        this.usuarioActor = usuarioActor;
        this.accion = accion;
        this.tipoEntidad = tipoEntidad;
        this.idEntidad = idEntidad;
        this.valoresAnterioresJson = valoresAnterioresJson;
        this.valoresNuevosJson = valoresNuevosJson;
        this.direccionIp = direccionIp;
    }

    public UUID getId() {
        return id;
    }

    public UsuarioAppEntidad getUsuarioActor() {
        return usuarioActor;
    }

    public String getAccion() {
        return accion;
    }

    public String getTipoEntidad() {
        return tipoEntidad;
    }

    public UUID getIdEntidad() {
        return idEntidad;
    }

    public String getValoresAnterioresJson() {
        return valoresAnterioresJson;
    }

    public String getValoresNuevosJson() {
        return valoresNuevosJson;
    }

    public String getDireccionIp() {
        return direccionIp;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }
}
