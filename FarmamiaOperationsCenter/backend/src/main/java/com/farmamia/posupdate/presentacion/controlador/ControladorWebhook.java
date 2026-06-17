package com.farmamia.posupdate.presentacion.controlador;

import com.farmamia.posupdate.aplicacion.casouso.ProcesarAlertaRedCasoUso;
import com.farmamia.posupdate.presentacion.dto.PayloadWebhookAlertmanager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
public class ControladorWebhook {

    private final ProcesarAlertaRedCasoUso procesarAlertaRedCasoUso;

    public ControladorWebhook(ProcesarAlertaRedCasoUso procesarAlertaRedCasoUso) {
        this.procesarAlertaRedCasoUso = procesarAlertaRedCasoUso;
    }

    @PostMapping("/alertmanager")
    public ResponseEntity<Void> recibirAlertaRed(@RequestBody PayloadWebhookAlertmanager payload) {
        procesarAlertaRedCasoUso.procesar(payload);
        return ResponseEntity.ok().build();
    }
}
