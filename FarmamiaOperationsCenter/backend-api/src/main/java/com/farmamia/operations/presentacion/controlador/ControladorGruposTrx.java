package com.farmamia.operations.presentacion.controlador;

import com.farmamia.operations.aplicacion.casouso.GestionarAuditoriaCasoUso;
import com.farmamia.operations.aplicacion.casouso.GestionarGruposTrxCasoUso;
import com.farmamia.operations.dominio.modelo.DatosAuditoria;
import com.farmamia.operations.dominio.modelo.DatosGrupoTrx;
import com.farmamia.operations.dominio.modelo.DetalleGrupoTrx;
import com.farmamia.operations.dominio.modelo.EquipoGrupoTrx;
import com.farmamia.operations.dominio.modelo.FiltroGruposTrx;
import com.farmamia.operations.dominio.modelo.GrupoTrx;
import com.farmamia.operations.dominio.modelo.Pagina;
import com.farmamia.operations.presentacion.dto.RespuestaEquipoGrupoTrx;
import com.farmamia.operations.presentacion.dto.RespuestaGrupoTrx;
import com.farmamia.operations.presentacion.dto.RespuestaPagina;
import com.farmamia.operations.presentacion.dto.SolicitudGrupoTrx;
import com.farmamia.operations.presentacion.dto.SolicitudMotivoOperacion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/grupos-trx")
public class ControladorGruposTrx {

    private final GestionarGruposTrxCasoUso gestionarGruposTrxCasoUso;
    private final GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso;

    public ControladorGruposTrx(
        GestionarGruposTrxCasoUso gestionarGruposTrxCasoUso,
        GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso
    ) {
        this.gestionarGruposTrxCasoUso = gestionarGruposTrxCasoUso;
        this.gestionarAuditoriaCasoUso = gestionarAuditoriaCasoUso;
    }

    @GetMapping
    public List<RespuestaGrupoTrx> listar() {
        return gestionarGruposTrxCasoUso.listar()
            .stream()
            .map(this::aRespuesta)
            .toList();
    }

    @GetMapping("/page")
    public RespuestaPagina<RespuestaGrupoTrx> listarPaginado(
        @RequestParam(required = false) String codigo,
        @RequestParam(required = false) String estado,
        @RequestParam(required = false) Boolean activo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(defaultValue = "codigo,asc") String sort
    ) {
        Pagina<GrupoTrx> pagina = gestionarGruposTrxCasoUso.listarPaginado(new FiltroGruposTrx(
            codigo,
            estado,
            activo,
            page,
            size,
            sort
        ));
        return new RespuestaPagina<>(
            pagina.contenido().stream().map(this::aRespuesta).toList(),
            pagina.pagina(),
            pagina.tamano(),
            pagina.totalElementos(),
            pagina.totalPaginas(),
            pagina.tieneSiguiente()
        );
    }

    @GetMapping("/{id}")
    public RespuestaGrupoTrx obtener(@PathVariable UUID id) {
        return aRespuesta(gestionarGruposTrxCasoUso.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaGrupoTrx crear(
        @Valid @RequestBody SolicitudGrupoTrx solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        GrupoTrx grupo = gestionarGruposTrxCasoUso.crear(aDatos(solicitud));
        auditar(autenticacion, request, "CREAR_GRUPO_TRX", grupo.id(), null, valoresGrupo(grupo, null));
        return aRespuesta(grupo);
    }

    @PutMapping("/{id}")
    public RespuestaGrupoTrx actualizar(
        @PathVariable UUID id,
        @Valid @RequestBody SolicitudGrupoTrx solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        GrupoTrx anterior = gestionarGruposTrxCasoUso.obtener(id).grupo();
        GrupoTrx grupo = gestionarGruposTrxCasoUso.actualizar(id, aDatos(solicitud));
        auditar(autenticacion, request, "MODIFICAR_GRUPO_TRX", grupo.id(), valoresGrupo(anterior, null), valoresGrupo(grupo, null));
        return aRespuesta(grupo);
    }

    @PostMapping("/{id}/pausar")
    public RespuestaGrupoTrx pausar(
        @PathVariable UUID id,
        @RequestBody(required = false) SolicitudMotivoOperacion solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        GrupoTrx anterior = gestionarGruposTrxCasoUso.obtener(id).grupo();
        GrupoTrx grupo = gestionarGruposTrxCasoUso.pausar(id);
        auditar(autenticacion, request, "PAUSAR_GRUPO_TRX", grupo.id(), valoresGrupo(anterior, motivo(solicitud)), valoresGrupo(grupo, motivo(solicitud)));
        return aRespuesta(grupo);
    }

    @PostMapping("/{id}/reanudar")
    public RespuestaGrupoTrx reanudar(
        @PathVariable UUID id,
        @RequestBody(required = false) SolicitudMotivoOperacion solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        GrupoTrx anterior = gestionarGruposTrxCasoUso.obtener(id).grupo();
        GrupoTrx grupo = gestionarGruposTrxCasoUso.reanudar(id);
        auditar(autenticacion, request, "REANUDAR_GRUPO_TRX", grupo.id(), valoresGrupo(anterior, motivo(solicitud)), valoresGrupo(grupo, motivo(solicitud)));
        return aRespuesta(grupo);
    }

    @PostMapping("/{id}/retirar")
    public RespuestaGrupoTrx retirar(
        @PathVariable UUID id,
        @RequestBody(required = false) SolicitudMotivoOperacion solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        GrupoTrx anterior = gestionarGruposTrxCasoUso.obtener(id).grupo();
        GrupoTrx grupo = gestionarGruposTrxCasoUso.retirar(id);
        auditar(autenticacion, request, "RETIRAR_GRUPO_TRX", grupo.id(), valoresGrupo(anterior, motivo(solicitud)), valoresGrupo(grupo, motivo(solicitud)));
        return aRespuesta(grupo);
    }

    @PostMapping("/{id}/equipos/{equipoId}")
    public RespuestaGrupoTrx asignarEquipo(
        @PathVariable UUID id,
        @PathVariable UUID equipoId,
        @RequestBody(required = false) SolicitudMotivoOperacion solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        GrupoTrx grupo = gestionarGruposTrxCasoUso.asignarEquipo(id, equipoId);
        auditar(autenticacion, request, "ASIGNAR_EQUIPO_TRX", grupo.id(), null, Map.of(
            "grupoTrx", grupo.codigo(),
            "equipoId", equipoId,
            "motivo", motivo(solicitud)
        ));
        return aRespuesta(gestionarGruposTrxCasoUso.obtener(id));
    }

    @DeleteMapping("/{id}/equipos/{equipoId}")
    public RespuestaGrupoTrx quitarEquipo(
        @PathVariable UUID id,
        @PathVariable UUID equipoId,
        @RequestBody(required = false) SolicitudMotivoOperacion solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        GrupoTrx grupo = gestionarGruposTrxCasoUso.quitarEquipo(id, equipoId);
        auditar(autenticacion, request, "QUITAR_EQUIPO_TRX", grupo.id(), Map.of(
            "grupoTrx", grupo.codigo(),
            "equipoId", equipoId,
            "motivo", motivo(solicitud)
        ), null);
        return aRespuesta(gestionarGruposTrxCasoUso.obtener(id));
    }

    private DatosGrupoTrx aDatos(SolicitudGrupoTrx solicitud) {
        return new DatosGrupoTrx(
            solicitud.codigo(),
            solicitud.nombre(),
            solicitud.descripcion(),
            solicitud.maximoEquipos(),
            solicitud.activo()
        );
    }

    private RespuestaGrupoTrx aRespuesta(DetalleGrupoTrx detalle) {
        GrupoTrx grupo = detalle.grupo();
        return new RespuestaGrupoTrx(
            grupo.id(),
            grupo.codigo(),
            grupo.nombre(),
            grupo.descripcion(),
            grupo.estado().name(),
            grupo.maximoEquipos(),
            grupo.activo(),
            grupo.equiposAsignados(),
            grupo.farmaciasInvolucradas(),
            grupo.creadoEn(),
            grupo.actualizadoEn(),
            detalle.equipos().stream().map(this::aRespuestaEquipo).toList(),
            detalle.codigosFarmacia()
        );
    }

    private RespuestaGrupoTrx aRespuesta(GrupoTrx grupo) {
        return new RespuestaGrupoTrx(
            grupo.id(),
            grupo.codigo(),
            grupo.nombre(),
            grupo.descripcion(),
            grupo.estado().name(),
            grupo.maximoEquipos(),
            grupo.activo(),
            grupo.equiposAsignados(),
            grupo.farmaciasInvolucradas(),
            grupo.creadoEn(),
            grupo.actualizadoEn(),
            List.of(),
            List.of()
        );
    }

    private RespuestaEquipoGrupoTrx aRespuestaEquipo(EquipoGrupoTrx equipo) {
        return new RespuestaEquipoGrupoTrx(
            equipo.idEquipo(),
            equipo.nombreEquipo(),
            equipo.idFarmacia(),
            equipo.codigoFarmacia(),
            equipo.nombreFarmacia(),
            equipo.versionPos(),
            equipo.estadoEquipo(),
            equipo.ultimoLatidoEn(),
            equipo.asignadoEn()
        );
    }

    private void auditar(
        Authentication autenticacion,
        HttpServletRequest request,
        String accion,
        UUID idEntidad,
        Map<String, Object> valoresAnteriores,
        Map<String, Object> valoresNuevos
    ) {
        gestionarAuditoriaCasoUso.registrar(new DatosAuditoria(
            usuario(autenticacion),
            accion,
            "GRUPO_TRX",
            idEntidad,
            valoresAnteriores,
            valoresNuevos,
            direccionIp(request)
        ));
    }

    private Map<String, Object> valoresGrupo(GrupoTrx grupo, String motivo) {
        return Map.of(
            "codigo", grupo.codigo(),
            "nombre", grupo.nombre(),
            "estado", grupo.estado().name(),
            "maximoEquipos", grupo.maximoEquipos(),
            "activo", grupo.activo(),
            "motivo", motivo == null ? "" : motivo
        );
    }

    private String motivo(SolicitudMotivoOperacion solicitud) {
        return solicitud == null || solicitud.motivo() == null ? "" : solicitud.motivo();
    }

    private String usuario(Authentication autenticacion) {
        return autenticacion == null ? null : autenticacion.getName();
    }

    private void exigirOperador(Authentication autenticacion) {
        PermisosAdministrativos.exigirRol(
            autenticacion,
            "Solo ADMIN u OPERATOR pueden operar Grupos TRX.",
            "ADMIN",
            "OPERATOR"
        );
    }

    private String direccionIp(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
