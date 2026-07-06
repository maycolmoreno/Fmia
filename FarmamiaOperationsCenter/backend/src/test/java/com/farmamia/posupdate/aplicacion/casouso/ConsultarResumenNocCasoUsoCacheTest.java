package com.farmamia.posupdate.aplicacion.casouso;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.farmamia.posupdate.dominio.modelo.ResumenNocDashboard;
import com.farmamia.posupdate.dominio.puerto.RepositorioEstadoFarmacias;
import com.farmamia.posupdate.dominio.puerto.RepositorioResumenNoc;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Verifica que /api/dashboard/resumen-noc (el "estado-operacional" del NOC que el panel consulta
// cada 30s desde cada sesion abierta) efectivamente comparte resultado entre llamadas dentro del
// TTL, en vez de recalcular todo en cada poll de cada usuario (hallazgo de la auditoria). Usa un
// ApplicationContext minimo (no @SpringBootTest) solo con cache + el caso de uso + repos mockeados,
// porque @Cacheable es un proxy AOP: instanciar la clase con "new" no lo activaria.
class ConsultarResumenNocCasoUsoCacheTest {

    private AnnotationConfigApplicationContext contexto;

    @AfterEach
    void cerrarContexto() {
        if (contexto != null) {
            contexto.close();
        }
    }

    @Test
    void obtenerReutilizaResultadoCacheadoDentroDelTtl() {
        RepositorioEstadoFarmacias repositorioEstadoFarmacias = mock(RepositorioEstadoFarmacias.class);
        RepositorioResumenNoc repositorioResumenNoc = mock(RepositorioResumenNoc.class);
        when(repositorioEstadoFarmacias.listar()).thenReturn(List.of());
        when(repositorioResumenNoc.obtenerEstadoRed())
            .thenReturn(new ResumenNocDashboard.EstadoRedNoc(0, 0, 0));
        when(repositorioResumenNoc.obtenerEstadoPos())
            .thenReturn(new ResumenNocDashboard.EstadoPosNoc(0, 0, 0, 0, null));
        when(repositorioResumenNoc.obtenerCampanaActiva()).thenReturn(null);
        when(repositorioResumenNoc.obtenerAlertasRecientes(10)).thenReturn(List.of());

        contexto = new AnnotationConfigApplicationContext();
        contexto.registerBean(RepositorioEstadoFarmacias.class, () -> repositorioEstadoFarmacias);
        contexto.registerBean(RepositorioResumenNoc.class, () -> repositorioResumenNoc);
        contexto.register(ConfiguracionCacheDePrueba.class, ConsultarResumenNocCasoUso.class);
        contexto.refresh();

        ConsultarResumenNocCasoUso casoUso = contexto.getBean(ConsultarResumenNocCasoUso.class);

        ResumenNocDashboard primeraLlamada = casoUso.obtener();
        ResumenNocDashboard segundaLlamada = casoUso.obtener();

        assertSame(primeraLlamada, segundaLlamada, "la segunda llamada deberia devolver el mismo objeto cacheado");
        verify(repositorioEstadoFarmacias, times(1)).listar();
        verify(repositorioResumenNoc, times(1)).obtenerEstadoRed();
    }

    @Configuration
    @EnableCaching
    static class ConfiguracionCacheDePrueba {
        @Bean
        CaffeineCacheManager cacheManager() {
            CaffeineCacheManager cacheManager = new CaffeineCacheManager();
            cacheManager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(1)));
            return cacheManager;
        }
    }
}
