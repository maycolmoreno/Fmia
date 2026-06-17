package com.farmamia.posupdate.infraestructura.persistencia.adaptador;

import com.farmamia.posupdate.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.posupdate.dominio.puerto.RepositorioTokensAgente;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.TokenAgenteEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.TokenAgenteRepositorioJpa;
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
