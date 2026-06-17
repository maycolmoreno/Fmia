package com.farmamia.posupdate.infraestructura.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FiltroAutenticacionAdministrativa extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";
    private static final List<String> PREFIJOS_ADMIN = List.of(
        "/api/branches",
        "/api/farmacias",
        "/api/devices",
        "/api/equipos-pos",
        "/api/packages",
        "/api/versiones-pos",
        "/api/deployments",
        "/api/campanas-pos",
        "/api/orchestration",
        "/api/dashboard",
        "/api/update-events",
        "/api/eventos-agente",
        "/api/grupos-trx",
        "/api/alerts",
        "/api/audit-logs",
        "/api/admin"
    );

    private final ServicioJwtAdministrativo servicioJwtAdministrativo;
    private final ServicioAutenticacionAgente servicioAutenticacionAgente;

    public FiltroAutenticacionAdministrativa(
        ServicioJwtAdministrativo servicioJwtAdministrativo,
        ServicioAutenticacionAgente servicioAutenticacionAgente
    ) {
        this.servicioJwtAdministrativo = servicioJwtAdministrativo;
        this.servicioAutenticacionAgente = servicioAutenticacionAgente;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (!requiereTokenAdministrativo(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String autorizacion = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (autorizacion == null || !autorizacion.startsWith(PREFIJO_BEARER)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing admin bearer token");
            return;
        }

        String token = autorizacion.substring(PREFIJO_BEARER.length());

        try {
            if (esDescargaPaquete(request) && servicioAutenticacionAgente.validarToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            ServicioJwtAdministrativo.UsuarioAutenticado usuario = servicioJwtAdministrativo.validar(token);

            UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(
                usuario.usuario(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol()))
            );
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid admin bearer token");
        }
    }

    private boolean requiereTokenAdministrativo(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return false;
        }

        String ruta = request.getRequestURI();
        return PREFIJOS_ADMIN.stream().anyMatch(ruta::startsWith);
    }

    private boolean esDescargaPaquete(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return HttpMethod.GET.matches(request.getMethod())
            && ((ruta.startsWith("/api/packages/") && ruta.endsWith("/download"))
                || (ruta.startsWith("/api/versiones-pos/") && ruta.endsWith("/descargar")));
    }
}
