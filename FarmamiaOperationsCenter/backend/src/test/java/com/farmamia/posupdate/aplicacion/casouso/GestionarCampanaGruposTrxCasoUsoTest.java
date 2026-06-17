package com.farmamia.posupdate.aplicacion.casouso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void pausaGrupoTrxDentroDeCampanaBloqueaInstrucciones() {
        RepositorioCampanaGruposTrxFake repositorio = new RepositorioCampanaGruposTrxFake();
        GestionarCampanaGruposTrxCasoUso casoUso = new GestionarCampanaGruposTrxCasoUso(repositorio);

        CampanaGrupoTrx pausado = casoUso.pausar(repositorio.idCampana, repositorio.idGrupoTrx, "farmacias criticas");

        assertEquals(EstadoCampanaGrupoTrx.PAUSADO, pausado.estado());
        assertEquals("farmacias criticas", repositorio.motivo);
        assertTrue(repositorio.instruccionBloqueada(repositorio.idCampana, repositorio.idGrupoTrx, null));
    }

    private static final class RepositorioCampanaGruposTrxFake implements RepositorioCampanaGruposTrx {

        private final UUID idCampana = UUID.randomUUID();
        private final UUID idGrupoTrx = UUID.randomUUID();
        private EstadoCampanaGrupoTrx estado = EstadoCampanaGrupoTrx.EN_RIESGO;
        private String motivo;

        @Override
        public ResumenCampanaGruposTrx estadoPorTrx(UUID idCampana) {
            return new ResumenCampanaGruposTrx(
                idCampana,
                "Campana POS",
                "2026.08.01",
                "RUNNING",
                1,
                1,
                estado == EstadoCampanaGrupoTrx.PAUSADO ? 1 : 0,
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
        public CampanaGrupoTrx pausar(UUID idCampana, UUID idGrupoTrx, String motivo) {
            this.estado = EstadoCampanaGrupoTrx.PAUSADO;
            this.motivo = motivo;
            return grupo();
        }

        @Override
        public CampanaGrupoTrx reanudar(UUID idCampana, UUID idGrupoTrx) {
            this.estado = EstadoCampanaGrupoTrx.PENDIENTE;
            return grupo();
        }

        @Override
        public boolean instruccionBloqueada(UUID idCampana, UUID idGrupoTrx, String codigoGrupoLegacy) {
            return estado == EstadoCampanaGrupoTrx.PAUSADO;
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
                motivo,
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
