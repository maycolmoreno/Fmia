package com.farmamia.posupdate.infraestructura.seguridad;

import com.farmamia.posupdate.infraestructura.persistencia.entidad.TokenAgenteEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.TokenAgenteRepositorioJpa;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ServicioAutenticacionAgente {

    private final TokenAgenteRepositorioJpa tokenAgenteRepositorio;
    private final ServicioHashToken servicioHashToken;

    public ServicioAutenticacionAgente(
        TokenAgenteRepositorioJpa tokenAgenteRepositorio,
        ServicioHashToken servicioHashToken
    ) {
        this.tokenAgenteRepositorio = tokenAgenteRepositorio;
        this.servicioHashToken = servicioHashToken;
    }

    @Transactional
    public boolean validarToken(String tokenPlano) {
        String hashToken = servicioHashToken.sha256(tokenPlano);

        return tokenAgenteRepositorio.findByHashTokenAndRevocadoEnIsNull(hashToken)
            .map(this::registrarUsoValido)
            .orElse(false);
    }

    private boolean registrarUsoValido(TokenAgenteEntidad token) {
        token.registrarUso();
        return true;
    }
}
