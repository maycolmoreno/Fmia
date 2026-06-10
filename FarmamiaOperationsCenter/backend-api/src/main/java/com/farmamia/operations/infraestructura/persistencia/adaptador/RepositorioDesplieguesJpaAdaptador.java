package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.modelo.DatosCrearDespliegue;
import com.farmamia.operations.dominio.modelo.Despliegue;
import com.farmamia.operations.dominio.modelo.EstadoDespliegue;
import com.farmamia.operations.dominio.puerto.RepositorioDespliegues;
import com.farmamia.operations.infraestructura.persistencia.entidad.DespliegueEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.PaquetePosEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.DespliegueRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.PaquetePosRepositorioJpa;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioDesplieguesJpaAdaptador implements RepositorioDespliegues {

    private final DespliegueRepositorioJpa despliegueRepositorioJpa;
    private final ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa;
    private final PaquetePosRepositorioJpa paquetePosRepositorioJpa;
    private final EquipoRepositorioJpa equipoRepositorioJpa;

    public RepositorioDesplieguesJpaAdaptador(
        DespliegueRepositorioJpa despliegueRepositorioJpa,
        ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa,
        PaquetePosRepositorioJpa paquetePosRepositorioJpa,
        EquipoRepositorioJpa equipoRepositorioJpa
    ) {
        this.despliegueRepositorioJpa = despliegueRepositorioJpa;
        this.objetivoDespliegueRepositorioJpa = objetivoDespliegueRepositorioJpa;
        this.paquetePosRepositorioJpa = paquetePosRepositorioJpa;
        this.equipoRepositorioJpa = equipoRepositorioJpa;
    }

    @Override
    public Despliegue crear(DatosCrearDespliegue datos) {
        PaquetePosEntidad paquete = paquetePosRepositorioJpa.findById(datos.idPaquete())
            .orElseThrow(() -> new RecursoNoEncontradoException("Paquete POS no encontrado: " + datos.idPaquete()));

        if (!paquete.estaAprobado()) {
            throw new IllegalArgumentException("El paquete POS debe estar aprobado para crear un despliegue");
        }

        DespliegueEntidad despliegue = despliegueRepositorioJpa.save(new DespliegueEntidad(
            paquete,
            datos.nombre(),
            datos.descripcion(),
            datos.programadoEn()
        ));

        List<ObjetivoDespliegueEntidad> objetivos = datos.idsEquipos()
            .stream()
            .distinct()
            .map(idEquipo -> crearObjetivo(datos, despliegue, paquete, idEquipo))
            .toList();
        objetivoDespliegueRepositorioJpa.saveAll(objetivos);

        return aDominio(despliegue, objetivos.size());
    }

    @Override
    public List<Despliegue> listar() {
        return despliegueRepositorioJpa.findAll()
            .stream()
            .map(despliegue -> aDominio(despliegue, objetivoDespliegueRepositorioJpa.countByDespliegue_Id(despliegue.getId())))
            .toList();
    }

    @Override
    public Despliegue obtener(UUID id) {
        DespliegueEntidad despliegue = buscarDespliegue(id);
        return aDominio(despliegue, objetivoDespliegueRepositorioJpa.countByDespliegue_Id(id));
    }

    @Override
    public Despliegue programar(UUID id, OffsetDateTime programadoEn) {
        DespliegueEntidad despliegue = buscarDespliegue(id);
        despliegue.programar(programadoEn);
        return aDominio(despliegue, objetivoDespliegueRepositorioJpa.countByDespliegue_Id(id));
    }

    @Override
    public Despliegue pausar(UUID id) {
        DespliegueEntidad despliegue = buscarDespliegue(id);
        despliegue.pausar();
        return aDominio(despliegue, objetivoDespliegueRepositorioJpa.countByDespliegue_Id(id));
    }

    @Override
    public Despliegue reanudar(UUID id) {
        DespliegueEntidad despliegue = buscarDespliegue(id);
        despliegue.reanudar();
        return aDominio(despliegue, objetivoDespliegueRepositorioJpa.countByDespliegue_Id(id));
    }

    @Override
    public Despliegue cancelar(UUID id) {
        DespliegueEntidad despliegue = buscarDespliegue(id);
        despliegue.cancelar();
        return aDominio(despliegue, objetivoDespliegueRepositorioJpa.countByDespliegue_Id(id));
    }

    @Override
    public EstadoDespliegue estado(UUID id) {
        DespliegueEntidad despliegue = buscarDespliegue(id);
        Map<String, Long> conteo = objetivoDespliegueRepositorioJpa.findByDespliegue_Id(id)
            .stream()
            .collect(Collectors.groupingBy(ObjetivoDespliegueEntidad::getEstado, Collectors.counting()));

        return new EstadoDespliegue(id, despliegue.getEstado(), conteo);
    }

    private ObjetivoDespliegueEntidad crearObjetivo(
        DatosCrearDespliegue datos,
        DespliegueEntidad despliegue,
        PaquetePosEntidad paquete,
        UUID idEquipo
    ) {
        EquipoEntidad equipo = equipoRepositorioJpa.findById(idEquipo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado: " + idEquipo));

        return new ObjetivoDespliegueEntidad(
            despliegue,
            equipo,
            datos.grupoObjetivo(),
            datos.piloto(),
            paquete.getVersion()
        );
    }

    private DespliegueEntidad buscarDespliegue(UUID id) {
        return despliegueRepositorioJpa.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Despliegue no encontrado: " + id));
    }

    private Despliegue aDominio(DespliegueEntidad despliegue, long cantidadObjetivos) {
        return new Despliegue(
            despliegue.getId(),
            despliegue.getPaquete().getId(),
            despliegue.getPaquete().getVersion(),
            despliegue.getNombre(),
            despliegue.getDescripcion(),
            despliegue.getEstado(),
            despliegue.getProgramadoEn(),
            despliegue.getCreadoEn(),
            cantidadObjetivos
        );
    }
}
