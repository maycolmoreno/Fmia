package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.farmamia.operations.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.operations.dominio.modelo.DatosCrearDespliegue;
import com.farmamia.operations.dominio.modelo.Despliegue;
import com.farmamia.operations.dominio.modelo.EstadoDespliegue;
import com.farmamia.operations.dominio.modelo.FiltroDespliegues;
import com.farmamia.operations.dominio.modelo.Pagina;
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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Repository
public class RepositorioDesplieguesJpaAdaptador implements RepositorioDespliegues {

    private static final List<String> ESTADOS_COMPLETADOS = List.of("COMPLETED");
    private static final List<String> ESTADOS_FALLIDOS = List.of("FAILED", "ROLLBACK_FAILED");
    private static final List<String> ESTADOS_FINALES = List.of(
        "COMPLETED",
        "FAILED",
        "ROLLBACK_COMPLETED",
        "ROLLBACK_FAILED",
        "SKIPPED"
    );

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
    public Pagina<Despliegue> listarPaginado(FiltroDespliegues filtro) {
        org.springframework.data.domain.Page<DespliegueEntidad> pagina = despliegueRepositorioJpa.buscarConFiltros(
            minusculaANulo(filtro.q()),
            minusculaANulo(filtro.estado()),
            minusculaANulo(filtro.versionPaquete()),
            filtro.creadoDesde(),
            filtro.creadoHasta(),
            PageRequest.of(filtro.pagina(), filtro.tamano(), aOrden(filtro.orden()))
        );

        return new Pagina<>(
            pagina.getContent()
                .stream()
                .map(despliegue -> aDominio(despliegue, objetivoDespliegueRepositorioJpa.countByDespliegue_Id(despliegue.getId())))
                .toList(),
            pagina.getNumber(),
            pagina.getSize(),
            pagina.getTotalElements(),
            pagina.getTotalPages(),
            pagina.hasNext()
        );
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

        long total = conteo.values().stream().mapToLong(Long::longValue).sum();
        long completados = sumarEstados(conteo, ESTADOS_COMPLETADOS);
        long fallidos = sumarEstados(conteo, ESTADOS_FALLIDOS);
        long finales = sumarEstados(conteo, ESTADOS_FINALES);
        long pendientes = Math.max(0, total - finales);

        return new EstadoDespliegue(
            id,
            despliegue.getEstado(),
            total,
            completados,
            fallidos,
            pendientes,
            porcentaje(completados, total),
            porcentaje(fallidos, total),
            conteo
        );
    }

    private long sumarEstados(Map<String, Long> conteo, List<String> estados) {
        return estados.stream()
            .mapToLong(estado -> conteo.getOrDefault(estado, 0L))
            .sum();
    }

    private double porcentaje(long valor, long total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round((valor * 10000.0) / total) / 100.0;
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

    private Sort aOrden(String orden) {
        String[] partes = orden == null ? new String[0] : orden.split(",", 2);
        String campo = partes.length > 0 ? partes[0] : "creadoEn";
        Sort.Direction direccion = partes.length > 1 && "asc".equalsIgnoreCase(partes[1])
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        return Sort.by(direccion, switch (campo) {
            case "name", "nombre" -> "nombre";
            case "status", "estado" -> "estado";
            case "scheduledAt", "programadoEn" -> "programadoEn";
            default -> "creadoEn";
        });
    }

    private String minusculaANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase(Locale.ROOT);
    }
}
