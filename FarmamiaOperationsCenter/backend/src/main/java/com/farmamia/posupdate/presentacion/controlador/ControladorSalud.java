package com.farmamia.posupdate.presentacion.controlador;

import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class ControladorSalud {

    @GetMapping
    public Map<String, Object> consultarSalud() {
        return Map.of(
            "status", "UP",
            "service", "farmamia-operations-api",
            "serverTime", OffsetDateTime.now()
        );
    }
}
