package com.farmamia.posupdate.infraestructura.snmp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SnmpPollingServiceTest {

    @Test
    void calculaPorcentajeRam() {
        assertThat(SnmpPollingService.calcularPorcentajeRam(95, 100)).isEqualTo(95);
        assertThat(SnmpPollingService.calcularPorcentajeRam(1, 0)).isNull();
    }

    @Test
    void cpuAlertaEnLaSegundaLecturaAlta() {
        SnmpPollingService service = new SnmpPollingService(null, null, null);
        UUID idEquipo = UUID.randomUUID();

        assertThat(service.debeAlertarCpu(idEquipo, 91)).isFalse();
        assertThat(service.debeAlertarCpu(idEquipo, 92)).isTrue();
        assertThat(service.debeAlertarCpu(idEquipo, 93)).isFalse();
        assertThat(service.debeAlertarCpu(idEquipo, 80)).isFalse();
        assertThat(service.debeAlertarCpu(idEquipo, 91)).isFalse();
        assertThat(service.debeAlertarCpu(idEquipo, 91)).isTrue();
    }
}
