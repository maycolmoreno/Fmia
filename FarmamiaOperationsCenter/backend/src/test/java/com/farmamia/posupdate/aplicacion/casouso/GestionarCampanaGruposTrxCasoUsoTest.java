package com.farmamia.posupdate.aplicacion.casouso;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.farmamia.posupdate.dominio.modelo.CampanaGrupoTrx;
import com.farmamia.posupdate.dominio.modelo.EstadoCampanaGrupoTrx;
import com.farmamia.posupdate.dominio.modelo.ResumenCampanaGruposTrx;
import com.farmamia.posupdate.dominio.puerto.RepositorioCampanaGruposTrx;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GestionarCampanaGruposTrxCasoUsoTest {

    @Test
    void consultaEstadoPorTrxConMetricasDeFarmacia() {
        RepositorioCampanaGruposTrxFake repositorio = new RepositorioCampanaGruposTrxFake();
        GestionarCampanaGruposTrxCasoUso casoUso = new GestionarCampanaGruposTrxCasoUso(repositorio);

        ResumenCampanaGruposTrx resumen = casoUso.estadoPorTrx(repositorio.idCampana);

        assertEquals(8, resumen.farmaciasAfectadas());
        assertEquals(2, resumen.farmaciasTurnoAfectadas());
        assertEquals(3, resumen.farmaciasCriticas());
        assertEquals(EstadoCampanaGrupoTrx.EN_RIESGO, resumen.grupos().get(0).estado());
    }

    private static final class RepositorioCampanaGruposTrxFake implements RepositorioCampanaGruposTrx {

        private final UUID idCampana = UUID.randomUUID();
        private final UUID idGrupoTrx = UUID.randomUUID();
        private final EstadoCampanaGrupoTrx estado = EstadoCampanaGrupoTrx.EN_RIESGO;

        @Override
        public ResumenCampanaGruposTrx estadoPorTrx(UUID idCampana) {
            return new ResumenCampanaGruposTrx(
                idCampana,
                "Campana POS",
                "2026.08.01",
                "RUNNING",
                1,
                1,
                0,
                8,
                2,
                3,
                List.of(grupo())
            );
        }

        @Override
        public CampanaGrupoTrx asociar(UUID idCampana, UUID idGrupoTrx) {
            return grupo();
        }

        @Override
        public void quitar(UUID idCampana, UUID idGrupoTrx) {
        }

        @Override
        public boolean instruccionBloqueada(UUID idCampana, UUID idGrupoTrx, String codigoGrupoLegacy) {
            return false;
        }

        private CampanaGrupoTrx grupo() {
            return new CampanaGrupoTrx(
                UUID.randomUUID(),
                idCampana,
                "Campana POS",
                "2026.08.01",
                "RUNNING",
                idGrupoTrx,
                "trx001",
                "TRX 001",
                1,
                estado,
                42,
                8,
                2,
                3,
                12,
                4,
                126,
                95,
                20,
                11,
                2,
                null,
                "3 farmacias criticas y 2 farmacias de turno afectadas",
                OffsetDateTime.now(),
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                List.of()
            );
        }
    }
}
