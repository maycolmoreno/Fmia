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

@Entity
@Table(name = "agent_tokens")
public class TokenAgenteEntidad {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private EquipoEntidad equipo;

    @Column(name = "token_hash", nullable = false)
    private String hashToken;

    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private OffsetDateTime emitidoEn;

    @Column(name = "expires_at")
    private OffsetDateTime expiraEn;

    @Column(name = "revoked_at")
    private OffsetDateTime revocadoEn;

    @Column(name = "last_used_at")
    private OffsetDateTime ultimoUsoEn;

    protected TokenAgenteEntidad() {
    }

    public TokenAgenteEntidad(EquipoEntidad equipo, String hashToken) {
        this.equipo = equipo;
        this.hashToken = hashToken;
    }

    public void revocar() {
        this.revocadoEn = OffsetDateTime.now();
    }

    public void registrarUso() {
        this.ultimoUsoEn = OffsetDateTime.now();
    }
}
