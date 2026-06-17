package com.farmamia.posupdate.infraestructura.seguridad;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.UsuarioAppEntidad;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class ServicioJwtAdministrativo {

    private static final String ALGORITMO_HMAC = "HmacSHA256";
    private static final Base64.Encoder CODIFICADOR = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODIFICADOR = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secreto;
    private final Duration duracion;

    public ServicioJwtAdministrativo(
        ObjectMapper objectMapper,
        Environment environment,
        @Value("${farmamia.security.jwt-secret:dev-secret-change-me}") String secreto,
        @Value("${farmamia.security.jwt-expiration-minutes:480}") long minutosExpiracion
    ) {
        validarSecretoSeguro(environment, secreto);
        this.objectMapper = objectMapper;
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
        this.duracion = Duration.ofMinutes(minutosExpiracion);
    }

    private void validarSecretoSeguro(Environment environment, String secreto) {
        boolean perfilEstricto = Arrays.stream(environment.getActiveProfiles())
            .anyMatch(perfil -> "qa".equalsIgnoreCase(perfil)
                || "prod".equalsIgnoreCase(perfil)
                || "production".equalsIgnoreCase(perfil));

        if (perfilEstricto && (secreto == null || secreto.isBlank() || "dev-secret-change-me".equals(secreto))) {
            throw new IllegalStateException("farmamia.security.jwt-secret es obligatorio y debe ser seguro en QA/PROD");
        }
    }

    public TokenAdministrativo generar(UsuarioAppEntidad usuario) {
        Instant emitidoEn = Instant.now();
        Instant expiraEn = emitidoEn.plus(duracion);

        Map<String, Object> encabezado = Map.of(
            "alg", "HS256",
            "typ", "JWT"
        );
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", usuario.getUsuario());
        payload.put("name", usuario.getNombreCompleto());
        payload.put("role", usuario.getRol());
        payload.put("iat", emitidoEn.getEpochSecond());
        payload.put("exp", expiraEn.getEpochSecond());

        String token = codificar(encabezado) + "." + codificar(payload);
        token = token + "." + firmar(token);

        return new TokenAdministrativo(token, OffsetDateTime.ofInstant(expiraEn, ZoneOffset.UTC));
    }

    public UsuarioAutenticado validar(String token) {
        String[] partes = token == null ? new String[0] : token.split("\\.");
        if (partes.length != 3) {
            throw new IllegalArgumentException("Token administrativo invalido");
        }

        String contenidoFirmado = partes[0] + "." + partes[1];
        String firmaEsperada = firmar(contenidoFirmado);
        if (!firmaEsperada.equals(partes[2])) {
            throw new IllegalArgumentException("Firma de token administrativo invalida");
        }

        Map<String, Object> payload = decodificarPayload(partes[1]);
        long exp = ((Number) payload.getOrDefault("exp", 0)).longValue();
        if (Instant.now().getEpochSecond() >= exp) {
            throw new IllegalArgumentException("Token administrativo expirado");
        }

        return new UsuarioAutenticado(
            String.valueOf(payload.get("sub")),
            String.valueOf(payload.get("role")),
            String.valueOf(payload.get("name"))
        );
    }

    private String codificar(Map<String, Object> datos) {
        try {
            return CODIFICADOR.encodeToString(objectMapper.writeValueAsBytes(datos));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo generar token administrativo", ex);
        }
    }

    private Map<String, Object> decodificarPayload(String payload) {
        try {
            byte[] json = DECODIFICADOR.decode(payload);
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException("Payload de token administrativo invalido", ex);
        }
    }

    private String firmar(String contenido) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO_HMAC);
            mac.init(new SecretKeySpec(secreto, ALGORITMO_HMAC));
            return CODIFICADOR.encodeToString(mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar token administrativo", ex);
        }
    }

    public record TokenAdministrativo(String valor, OffsetDateTime expiraEn) {
    }

    public record UsuarioAutenticado(String usuario, String rol, String nombreCompleto) {
    }
}
