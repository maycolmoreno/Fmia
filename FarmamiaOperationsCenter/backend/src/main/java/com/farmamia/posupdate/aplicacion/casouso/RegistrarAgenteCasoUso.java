package com.farmamia.posupdate.aplicacion.casouso;

import com.farmamia.posupdate.dominio.modelo.DatosRegistroAgente;
import com.farmamia.posupdate.dominio.modelo.Equipo;
import com.farmamia.posupdate.dominio.modelo.RegistroAgente;
import com.farmamia.posupdate.dominio.puerto.HasherTokens;
import com.farmamia.posupdate.dominio.puerto.RepositorioEquipos;
import com.farmamia.posupdate.dominio.puerto.RepositorioSucursales;
import com.farmamia.posupdate.dominio.puerto.RepositorioTokensAgente;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RegistrarAgenteCasoUso {

    private final RepositorioSucursales repositorioSucursales;
    private final RepositorioEquipos repositorioEquipos;
    private final RepositorioTokensAgente repositorioTokensAgente;
    private final HasherTokens hasherTokens;
    private final SecureRandom secureRandom = new SecureRandom();

    public RegistrarAgenteCasoUso(
        RepositorioSucursales repositorioSucursales,
        RepositorioEquipos repositorioEquipos,
        RepositorioTokensAgente repositorioTokensAgente,
        HasherTokens hasherTokens
    ) {
        this.repositorioSucursales = repositorioSucursales;
        this.repositorioEquipos = repositorioEquipos;
        this.repositorioTokensAgente = repositorioTokensAgente;
        this.hasherTokens = hasherTokens;
    }

    @Transactional
    public RegistroAgente registrar(DatosRegistroAgente datosRegistro) {
        UUID idSucursal = null;
        if (datosRegistro.codigoSucursal() != null && !datosRegistro.codigoSucursal().isBlank()) {
            idSucursal = repositorioSucursales.obtenerOCrearPorCodigo(datosRegistro.codigoSucursal());
        }

        Equipo equipo = repositorioEquipos.registrarOActualizar(idSucursal, datosRegistro);
        String tokenPlano = generarToken();

        repositorioTokensAgente.renovarTokenActivo(equipo.id(), hasherTokens.sha256(tokenPlano));

        return new RegistroAgente(equipo.id(), tokenPlano, OffsetDateTime.now());
    }

    private String generarToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}
