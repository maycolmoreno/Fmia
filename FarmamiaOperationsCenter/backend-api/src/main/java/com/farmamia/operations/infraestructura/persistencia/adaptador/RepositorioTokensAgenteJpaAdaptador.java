package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.puerto.RepositorioTokensAgente;
import com.farmamia.operations.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.TokenAgenteEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.TokenAgenteRepositorioJpa;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioTokensAgenteJpaAdaptador implements RepositorioTokensAgente {

    private final TokenAgenteRepositorioJpa tokenAgenteRepositorioJpa;
    private final EquipoRepositorioJpa equipoRepositorioJpa;

    public RepositorioTokensAgenteJpaAdaptador(
        TokenAgenteRepositorioJpa tokenAgenteRepositorioJpa,
        EquipoRepositorioJpa equipoRepositorioJpa
    ) {
        this.tokenAgenteRepositorioJpa = tokenAgenteRepositorioJpa;
        this.equipoRepositorioJpa = equipoRepositorioJpa;
    }

    @Override
    public void renovarTokenActivo(UUID idEquipo, String hashToken) {
        EquipoEntidad equipo = equipoRepositorioJpa.findById(idEquipo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado: " + idEquipo));

        tokenAgenteRepositorioJpa.findByEquipoAndRevocadoEnIsNull(equipo)
            .ifPresent(token -> {
                token.revocar();
                tokenAgenteRepositorioJpa.flush();
            });
        tokenAgenteRepositorioJpa.save(new TokenAgenteEntidad(equipo, hashToken));
    }
}
