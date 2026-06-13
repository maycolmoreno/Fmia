package com.farmamia.operations.aplicacion.casouso;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.farmamia.operations.dominio.modelo.FiltroEstadoCampanaFarmacia;
import com.farmamia.operations.dominio.modelo.ResumenEstadoCampanaFarmacia;
import com.farmamia.operations.dominio.puerto.RepositorioEstadoCampanaFarmacia;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarEstadoCampanaFarmaciaCasoUsoTest {

    @Test
    void normalizaFiltrosDeLecturaOperacional() {
        RepositorioEstadoCampanaFarmaciaFake repositorio = new RepositorioEstadoCampanaFarmaciaFake();
        ConsultarEstadoCampanaFarmaciaCasoUso casoUso = new ConsultarEstadoCampanaFarmaciaCasoUso(repositorio);
        UUID idCampana = UUID.randomUUID();

        casoUso.consultar(idCampana, new FiltroEstadoCampanaFarmacia(
            " CRITICA ",
            " EN_RIESGO ",
            " trx001 ",
            true,
            " FM001 ",
            -10,
            500,
            ""
        ));

        assertEquals(idCampana, repositorio.idCampana);
        assertEquals("CRITICA", repositorio.filtro.estadoTecnico());
        assertEquals("EN_RIESGO", repositorio.filtro.estadoOperacional());
        assertEquals("trx001", repositorio.filtro.grupoTrx());
        assertEquals("FM001", repositorio.filtro.q());
        assertEquals(0, repositorio.filtro.pagina());
        assertEquals(200, repositorio.filtro.tamano());
        assertEquals("prioridad,asc", repositorio.filtro.orden());
    }

    private static final class RepositorioEstadoCampanaFarmaciaFake implements RepositorioEstadoCampanaFarmacia {

        private UUID idCampana;
        private FiltroEstadoCampanaFarmacia filtro;

        @Override
        public ResumenEstadoCampanaFarmacia consultar(UUID idCampana, FiltroEstadoCampanaFarmacia filtro) {
            this.idCampana = idCampana;
            this.filtro = filtro;
            return new ResumenEstadoCampanaFarmacia(
                idCampana,
                "Campana",
                "2026.08.01",
                "RUNNING",
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                null,
                filtro.pagina(),
                filtro.tamano(),
                0,
                0,
                false,
                List.of()
            );
        }
    }
}
