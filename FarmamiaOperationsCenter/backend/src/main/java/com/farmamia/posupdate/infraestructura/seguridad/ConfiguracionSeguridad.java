package com.farmamia.posupdate.infraestructura.seguridad;

import java.util.List;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    private final FiltroAutenticacionAgente filtroAutenticacionAgente;
    private final FiltroAutenticacionAdministrativa filtroAutenticacionAdministrativa;
    private final List<String> origenesPermitidosCors;

    public ConfiguracionSeguridad(
        FiltroAutenticacionAgente filtroAutenticacionAgente,
        FiltroAutenticacionAdministrativa filtroAutenticacionAdministrativa,
        @Value("${farmamia.security.cors-allowed-origins:http://localhost:4200,http://127.0.0.1:4200}") String origenesPermitidosCors
    ) {
        this.filtroAutenticacionAgente = filtroAutenticacionAgente;
        this.filtroAutenticacionAdministrativa = filtroAutenticacionAdministrativa;
        this.origenesPermitidosCors = Arrays.stream(origenesPermitidosCors.split(","))
            .map(String::trim)
            .filter(origen -> !origen.isBlank())
            .toList();
    }

    @Bean
    SecurityFilterChain cadenaFiltrosSeguridad(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(configuracionCors()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/api/health", "/api/auth/login", "/api/agent/register", "/api/webhooks/**").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                .requestMatchers("/api/packages/*/download", "/api/versiones-pos/*/descargar").permitAll()
                .requestMatchers("/api/branches/**", "/api/devices/**", "/api/packages/**").authenticated()
                .requestMatchers("/api/farmacias/**", "/api/equipos-pos/**", "/api/versiones-pos/**").authenticated()
                .requestMatchers("/api/deployments/**", "/api/orchestration/**", "/api/dashboard/**", "/api/update-events/**", "/api/alerts/**").authenticated()
                .requestMatchers("/api/campanas-pos/**", "/api/eventos-agente/**", "/api/grupos-trx/**").authenticated()
                .requestMatchers("/api/audit-logs/**", "/api/admin/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(filtroAutenticacionAdministrativa, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(filtroAutenticacionAgente, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    PasswordEncoder codificadorContrasenas() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource configuracionCors() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOriginPatterns(origenesPermitidosCors);
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("*"));
        configuracion.setExposedHeaders(List.of("Content-Disposition"));
        configuracion.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        fuente.registerCorsConfiguration("/actuator/**", configuracion);
        return fuente;
    }
}
