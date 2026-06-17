package com.farmamia.posupdate.aplicacion.casouso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.farmamia.posupdate.dominio.modelo.DatosGrupoTrx;
import com.farmamia.posupdate.dominio.modelo.DetalleGrupoTrx;
import com.farmamia.posupdate.dominio.modelo.EquipoGrupoTrx;
import com.farmamia.posupdate.dominio.modelo.EstadoGrupoTrx;
import com.farmamia.posupdate.dominio.modelo.FiltroGruposTrx;
import com.farmamia.posupdate.dominio.modelo.GrupoTrx;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.dominio.puerto.RepositorioGruposTrx;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GestionarGruposTrxCasoUsoTest {

    @Test
    void creaGrupoConCodigoUnicoYMaximoCien() {
        RepositorioGruposTrxEnMemoria repositorio = new RepositorioGruposTrxEnMemoria();
        GestionarGruposTrxCasoUso casoUso = new GestionarGruposTrxCasoUso(repositorio);

        GrupoTrx grupo = casoUso.crear(new DatosGrupoTrx("trx001", "TRX 001", "Oleada controlada", 100, true));

        assertEquals("trx001", grupo.codigo());
        assertEquals(EstadoGrupoTrx.ACTIVO, grupo.estado());
        assertEquals(100, grupo.maximoEquipos());
        assertThrows(IllegalArgumentException.class, () -> casoUso.crear(new DatosGrupoTrx("trx001", "Duplicado", null, 100, true)));
        assertThrows(IllegalArgumentException.class, () -> casoUso.crear(new DatosGrupoTrx("trx999", "Invalido", null, 101, true)));
    }

    @Test
    void pausaYReanudaGrupo() {
        RepositorioGruposTrxEnMemoria repositorio = new RepositorioGruposTrxEnMemoria();
        GestionarGruposTrxCasoUso casoUso = new GestionarGruposTrxCasoUso(repositorio);
        GrupoTrx grupo = casoUso.crear(new DatosGrupoTrx("trx001", "TRX 001", null, 100, true));

        assertEquals(EstadoGrupoTrx.PAUSADO, casoUso.pausar(grupo.id()).estado());
        assertEquals(EstadoGrupoTrx.ACTIVO, casoUso.reanudar(grupo.id()).estado());
    }

    @Test
    void noAsignaEquipoAGrupoPausado() {
        RepositorioGruposTrxEnMemoria repositorio = new RepositorioGruposTrxEnMemoria();
        GestionarGruposTrxCasoUso casoUso = new GestionarGruposTrxCasoUso(repositorio);
        GrupoTrx grupo = casoUso.crear(new DatosGrupoTrx("trx001", "TRX 001", null, 100, true));
        casoUso.pausar(grupo.id());

        assertThrows(IllegalArgumentException.class, () -> casoUso.asignarEquipo(grupo.id(), UUID.randomUUID()));
    }

    @Test
    void noPermiteExcederMaximoEquipos() {
        RepositorioGruposTrxEnMemoria repositorio = new RepositorioGruposTrxEnMemoria();
        GestionarGruposTrxCasoUso casoUso = new GestionarGruposTrxCasoUso(repositorio);
        GrupoTrx grupo = casoUso.crear(new DatosGrupoTrx("trx001", "TRX 001", null, 1, true));

        casoUso.asignarEquipo(grupo.id(), UUID.randomUUID());

        assertThrows(IllegalArgumentException.class, () -> casoUso.asignarEquipo(grupo.id(), UUID.randomUUID()));
    }

    private static final class RepositorioGruposTrxEnMemoria implements RepositorioGruposTrx {

        private final Map<UUID, GrupoTrx> grupos = new HashMap<>();
        private final Map<UUID, UUID> equipoAGrupo = new HashMap<>();

        @Override
        public GrupoTrx crear(DatosGrupoTrx datos) {
            GrupoTrx grupo = new GrupoTrx(
                UUID.randomUUID(),
                datos.codigo(),
                datos.nombre(),
                datos.descripcion(),
                EstadoGrupoTrx.ACTIVO,
                datos.maximoEquipos(),
                datos.activo() == null || datos.activo(),
                0,
                0,
                OffsetDateTime.now(),
                OffsetDateTime.now()
            );
            grupos.put(grupo.id(), grupo);
            return grupo;
        }

        @Override
        public GrupoTrx actualizar(UUID id, DatosGrupoTrx datos) {
            GrupoTrx anterior = grupos.get(id);
            GrupoTrx actualizado = new GrupoTrx(
                anterior.id(),
                datos.codigo(),
                datos.nombre(),
                datos.descripcion(),
                anterior.estado(),
                datos.maximoEquipos(),
                datos.activo() == null || datos.activo(),
                anterior.equiposAsignados(),
                anterior.farmaciasInvolucradas(),
                anterior.creadoEn(),
                OffsetDateTime.now()
            );
            grupos.put(id, actualizado);
            return actualizado;
        }

        @Override
        public GrupoTrx cambiarEstado(UUID id, EstadoGrupoTrx estado) {
            GrupoTrx anterior = grupos.get(id);
            GrupoTrx actualizado = new GrupoTrx(
                anterior.id(),
                anterior.codigo(),
                anterior.nombre(),
                anterior.descripcion(),
                estado,
                anterior.maximoEquipos(),
                estado != EstadoGrupoTrx.RETIRADO && anterior.activo(),
                anterior.equiposAsignados(),
                anterior.farmaciasInvolucradas(),
                anterior.creadoEn(),
                OffsetDateTime.now()
            );
            grupos.put(id, actualizado);
            return actualizado;
        }

        @Override
        public Optional<DetalleGrupoTrx> buscarDetallePorId(UUID id) {
            return Optional.ofNullable(grupos.get(id))
                .map(grupo -> new DetalleGrupoTrx(grupo, List.of(), List.of()));
        }

        @Override
        public Optional<GrupoTrx> buscarPorCodigo(String codigo) {
            return grupos.values().stream()
                .filter(grupo -> grupo.codigo().equals(codigo))
                .findFirst();
        }

        @Override
        public List<GrupoTrx> listar() {
            return new ArrayList<>(grupos.values());
        }

        @Override
        public Pagina<GrupoTrx> listarPaginado(FiltroGruposTrx filtro) {
            List<GrupoTrx> contenido = listar();
            return new Pagina<>(contenido, 0, contenido.size(), contenido.size(), 1, false);
        }

        @Override
        public GrupoTrx asignarEquipo(UUID idGrupo, UUID idEquipo) {
            equipoAGrupo.put(idEquipo, idGrupo);
            GrupoTrx anterior = grupos.get(idGrupo);
            long asignados = equipoAGrupo.values().stream().filter(idGrupo::equals).count();
            GrupoTrx actualizado = new GrupoTrx(
                anterior.id(),
                anterior.codigo(),
                anterior.nombre(),
                anterior.descripcion(),
                anterior.estado(),
                anterior.maximoEquipos(),
                anterior.activo(),
                asignados,
                anterior.farmaciasInvolucradas(),
                anterior.creadoEn(),
                OffsetDateTime.now()
            );
            grupos.put(idGrupo, actualizado);
            return actualizado;
        }

        @Override
        public GrupoTrx quitarEquipo(UUID idGrupo, UUID idEquipo) {
            equipoAGrupo.remove(idEquipo);
            return grupos.get(idGrupo);
        }
    }
}
