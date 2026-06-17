package com.farmamia.posupdate.infraestructura.seguridad;

import com.farmamia.posupdate.dominio.modelo.FirmaPaquetePos;
import com.farmamia.posupdate.dominio.puerto.FirmadorPaquetesPos;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.OffsetDateTime;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FirmadorPaquetesPosRsa implements FirmadorPaquetesPos {

    private static final String ALGORITMO = "SHA256withRSA";

    private final String idClave;
    private final PrivateKey clavePrivada;
    private final String clavePublicaPem;

    public FirmadorPaquetesPosRsa(
        @Value("${farmamia.paquetes.firma.key-id:dev-runtime-rsa}") String idClave
    ) {
        this.idClave = idClave;
        KeyPair par = generarParTemporal();
        this.clavePrivada = par.getPrivate();
        this.clavePublicaPem = aPem(par.getPublic());
    }

    @Override
    public FirmaPaquetePos firmarChecksum(String checksumSha256) {
        try {
            Signature signature = Signature.getInstance(ALGORITMO);
            signature.initSign(clavePrivada);
            signature.update(checksumSha256.getBytes(StandardCharsets.UTF_8));
            String firma = Base64.getEncoder().encodeToString(signature.sign());
            return new FirmaPaquetePos(firma, ALGORITMO, idClave, clavePublicaPem, OffsetDateTime.now(), "VALID");
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar el paquete POS", ex);
        }
    }

    private static KeyPair generarParTemporal() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(3072);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar clave RSA de firma POS", ex);
        }
    }

    private static String aPem(PublicKey clavePublica) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
            .encodeToString(clavePublica.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
    }
}
