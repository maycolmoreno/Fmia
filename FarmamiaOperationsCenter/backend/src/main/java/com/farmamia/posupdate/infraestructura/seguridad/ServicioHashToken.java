package com.farmamia.posupdate.infraestructura.seguridad;

import com.farmamia.posupdate.dominio.puerto.HasherTokens;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Service;

@Service
public class ServicioHashToken implements HasherTokens {

    @Override
    public String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return aHexadecimal(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String aHexadecimal(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte valor : bytes) {
            builder.append(String.format("%02x", valor));
        }
        return builder.toString();
    }
}
