package com.farmamia.operations.infraestructura.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FiltroAutenticacionAgente extends OncePerRequestFilter {

    private static final String PREFIJO_AGENTE = "/api/agent/";
    private static final String RUTA_REGISTRO = "/api/agent/register";
    private static final String PREFIJO_BEARER = "Bearer ";

    private final ServicioAutenticacionAgente servicioAutenticacionAgente;

    public FiltroAutenticacionAgente(ServicioAutenticacionAgente servicioAutenticacionAgente) {
        this.servicioAutenticacionAgente = servicioAutenticacionAgente;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String ruta = request.getRequestURI();

        if (requiereTokenAgente(ruta)) {
            String autorizacion = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (autorizacion == null || !autorizacion.startsWith(PREFIJO_BEARER)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing agent bearer token");
                return;
            }

            String token = autorizacion.substring(PREFIJO_BEARER.length());
            if (token.isBlank() || !servicioAutenticacionAgente.validarToken(token)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid agent bearer token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiereTokenAgente(String ruta) {
        return ruta.startsWith(PREFIJO_AGENTE) && !RUTA_REGISTRO.equals(ruta);
    }
}
