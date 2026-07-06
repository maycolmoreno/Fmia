package com.farmamia.posupdate.infraestructura.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// El NOC hace polling de /api/dashboard/resumen-noc cada 30s desde cada sesion de panel abierta;
// sin cache, ConsultarResumenNocCasoUso.obtener() recalcula todo (todas las farmacias, equipos,
// alertas y objetivos activos) en cada llamada de cada usuario. Un TTL corto (menor al intervalo
// de polling) comparte el resultado entre llamadas concurrentes sin arriesgar datos obsoletos.
@Configuration
@EnableCaching
public class ConfiguracionCache {

    @Bean
    public CacheManagerCustomizer<CaffeineCacheManager> personalizadorCacheResumenNoc(
        @Value("${farmamia.cache.resumen-noc.ttl-segundos:15}") long ttlSegundos
    ) {
        return cacheManager -> cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSegundos))
                .maximumSize(10)
        );
    }
}
