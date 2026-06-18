package com.farmamia.posupdate.infraestructura.persistencia.adaptador;

import com.farmamia.posupdate.aplicacion.excepcion.RecursoNoEncontradoException;
import com.farmamia.posupdate.dominio.modelo.DatosCrearDespliegue;
import com.farmamia.posupdate.dominio.modelo.Despliegue;
import com.farmamia.posupdate.dominio.modelo.EstadoDespliegue;
import com.farmamia.posupdate.dominio.modelo.FiltroDespliegues;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.dominio.puerto.RepositorioDespliegues;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.DespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.EquipoEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.GrupoTrxEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.entidad.PaquetePosEntidad;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.DespliegueRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.EquipoRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.GrupoTrxRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.ObjetivoDespliegueRepositorioJpa;
import com.farmamia.posupdate.infraestructura.persistencia.repositorio.PaquetePosRepositorioJpa;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Repository
public class RepositorioDesplieguesJpaAdaptador implements RepositorioDespliegues {

    private static final OffsetDateTime FECHA_NEUTRA = OffsetDateTime.parse("1970-01-01T00:00:00Z");
    private static final Pattern PATRON_GRUPO_TRX = Pattern.compile("^TRX[0-9]{3}$");

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
    private final GrupoTrxRepositorioJpa grupoTrxRepositorioJpa;

    public RepositorioDesplieguesJpaAdaptador(
        DespliegueRepositorioJpa despliegueRepositorioJpa,
        ObjetivoDespliegueRepositorioJpa objetivoDespliegueRepositorioJpa,
        PaquetePosRepositorioJpa paquetePosRepositorioJpa,
        EquipoRepositorioJpa equipoRepositorioJpa,
        GrupoTrxRepositorioJpa grupoTrxRepositorioJpa
    ) {
        this.despliegueRepositorioJpa = despliegueRepositorioJpa;
        this.objetivoDespliegueRepositorioJpa = objetivoDespliegueRepositorioJpa;
        this.paquetePosRepositorioJpa = paquetePosRepositorioJpa;
        this.equipoRepositorioJpa = equipoRepositorioJpa;
        this.grupoTrxRepositorioJpa = grupoTrxRepositorioJpa;
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

        GrupoTrxEntidad grupoTrxObjetivo = resolverGrupoTrxObjetivo(datos.grupoObjetivo());
        List<UUID> idsObjetivo = resolverIdsObjetivo(datos, grupoTrxObjetivo);
        if (idsObjetivo.isEmpty()) {
            throw new IllegalArgumentException("La campana debe tener al menos un equipo POS objetivo.");
        }

        List<ObjetivoDespliegueEntidad> objetivos = idsObjetivo
            .stream()
            .distinct()
            .map(idEquipo -> crearObjetivo(datos, despliegue, paquete, idEquipo, grupoTrxObjetivo))
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
        String q = minusculaANulo(filtro.q());
        String estado = minusculaANulo(filtro.estado());
        String versionPaquete = minusculaANulo(filtro.versionPaquete());
        org.springframework.data.domain.Page<DespliegueEntidad> pagina = despliegueRepositorioJpa.buscarConFiltros(
            q != null,
            nuloAValor(q),
            estado != null,
            nuloAValor(estado),
            versionPaquete != null,
            nuloAValor(versionPaquete),
            filtro.creadoDesde() != null,
            filtro.creadoDesde() == null ? FECHA_NEUTRA : filtro.creadoDesde(),
            filtro.creadoHasta() != null,
            filtro.creadoHasta() == null ? FECHA_NEUTRA : filtro.creadoHasta(),
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
    public Despliegue aprobar(UUID id) {
        DespliegueEntidad despliegue = buscarDespliegue(id);
        despliegue.aprobar(OffsetDateTime.now());
        return aDominio(despliegue, objetivoDespliegueRepositorioJpa.countByDespliegue_Id(id));
    }

    @Override
    public Despliegue lanzar(UUID id) {
        DespliegueEntidad despliegue = buscarDespliegue(id);
        despliegue.lanzar();
        return aDominio(despliegue, objetivoDespliegueRepositorioJpa.countByDespliegue_Id(id));
    }

    @Override
    public Despliegue expandir(UUID id) {
        DespliegueEntidad despliegue = buscarDespliegue(id);
        despliegue.expandir();
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

    @Override
    public int contarFarmaciasTurno(UUID id) {
        buscarDespliegue(id);
        return (int) objetivoDespliegueRepositorioJpa.findByDespliegue_Id(id)
            .stream()
            .filter(objetivo -> objetivo.getEquipo() != null)
            .map(ObjetivoDespliegueEntidad::getEquipo)
            .filter(equipo -> equipo.getSucursal() != null && equipo.getSucursal().isDeTurno())
            .map(equipo -> equipo.getSucursal().getId())
            .distinct()
            .count();
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
        UUID idEquipo,
        GrupoTrxEntidad grupoTrxObjetivo
    ) {
        EquipoEntidad equipo = equipoRepositorioJpa.findById(idEquipo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Equipo no encontrado: " + idEquipo));

        ObjetivoDespliegueEntidad objetivo = new ObjetivoDespliegueEntidad(
            despliegue,
            equipo,
            grupoTrxObjetivo == null ? datos.grupoObjetivo() : grupoTrxObjetivo.getCodigo(),
            datos.piloto(),
            paquete.getVersion()
        );
        if (grupoTrxObjetivo != null) {
            objetivo.asignarGrupoTrx(grupoTrxObjetivo);
        }
        return objetivo;
    }

    private GrupoTrxEntidad resolverGrupoTrxObjetivo(String grupoObjetivo) {
        String codigo = codigoGrupoTrx(grupoObjetivo);
        if (codigo == null) {
            return null;
        }
        return grupoTrxRepositorioJpa.findByCodigo(codigo)
            .orElseThrow(() -> new RecursoNoEncontradoException("Grupo TRX no encontrado: " + codigo));
    }

    private List<UUID> resolverIdsObjetivo(DatosCrearDespliegue datos, GrupoTrxEntidad grupoTrxObjetivo) {
        if (grupoTrxObjetivo != null) {
            return equipoRepositorioJpa.findIdsByGrupoTrxCodigo(grupoTrxObjetivo.getCodigo());
        }
        return datos.idsEquipos() == null ? List.of() : datos.idsEquipos();
    }

    private String codigoGrupoTrx(String grupoObjetivo) {
        if (grupoObjetivo == null || grupoObjetivo.isBlank()) {
            return null;
        }
        String codigo = grupoObjetivo.trim().toUpperCase(Locale.ROOT);
        return PATRON_GRUPO_TRX.matcher(codigo).matches() ? codigo : null;
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
            case "createdAt", "creadoEn" -> "creadoEn";
            default -> "creadoEn";
        });
    }

    private String minusculaANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase(Locale.ROOT);
    }

    private String nuloAValor(String valor) {
        return valor == null ? "" : valor;
    }
}
