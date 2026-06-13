package com.farmamia.operations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CentroOperacionesFarmamiaAplicacion {

    public static void main(String[] args) {
        SpringApplication.run(CentroOperacionesFarmamiaAplicacion.class, args);
    }
}
