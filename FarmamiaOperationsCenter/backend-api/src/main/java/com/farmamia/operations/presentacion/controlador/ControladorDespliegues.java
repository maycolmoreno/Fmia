package com.farmamia.operations.presentacion.controlador;

import com.farmamia.operations.aplicacion.casouso.GestionarAuditoriaCasoUso;
import com.farmamia.operations.aplicacion.casouso.ConsultarEstadoCampanaFarmaciaCasoUso;
import com.farmamia.operations.aplicacion.casouso.GestionarCampanaGruposTrxCasoUso;
import com.farmamia.operations.aplicacion.casouso.GestionarDesplieguesCasoUso;
import com.farmamia.operations.dominio.modelo.CampanaGrupoTrx;
import com.farmamia.operations.dominio.modelo.DatosCrearDespliegue;
import com.farmamia.operations.dominio.modelo.DatosAuditoria;
import com.farmamia.operations.dominio.modelo.Despliegue;
import com.farmamia.operations.dominio.modelo.EstadoDespliegue;
import com.farmamia.operations.dominio.modelo.EquipoEstadoCampanaFarmacia;
import com.farmamia.operations.dominio.modelo.EstadoCampanaFarmacia;
import com.farmamia.operations.dominio.modelo.FiltroEstadoCampanaFarmacia;
import com.farmamia.operations.dominio.modelo.FiltroDespliegues;
import com.farmamia.operations.dominio.modelo.Pagina;
import com.farmamia.operations.dominio.modelo.ResumenCampanaGruposTrx;
import com.farmamia.operations.dominio.modelo.ResumenEstadoCampanaFarmacia;
import com.farmamia.operations.presentacion.dto.RespuestaCampanaGrupoTrx;
import com.farmamia.operations.presentacion.dto.RespuestaDespliegue;
import com.farmamia.operations.presentacion.dto.RespuestaEquipoEstadoCampanaFarmacia;
import com.farmamia.operations.presentacion.dto.RespuestaEstadoCampanaFarmacia;
import com.farmamia.operations.presentacion.dto.RespuestaEstadoDespliegue;
import com.farmamia.operations.presentacion.dto.RespuestaPagina;
import com.farmamia.operations.presentacion.dto.RespuestaResumenCampanaGruposTrx;
import com.farmamia.operations.presentacion.dto.RespuestaResumenEstadoCampanaFarmacia;
import com.farmamia.operations.presentacion.dto.SolicitudCrearDespliegue;
import com.farmamia.operations.presentacion.dto.SolicitudMotivoOperacion;
import com.farmamia.operations.presentacion.dto.SolicitudProgramarDespliegue;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/deployments", "/api/campanas-pos"})
public class ControladorDespliegues {

    private final GestionarDesplieguesCasoUso gestionarDesplieguesCasoUso;
    private final ConsultarEstadoCampanaFarmaciaCasoUso consultarEstadoCampanaFarmaciaCasoUso;
    private final GestionarCampanaGruposTrxCasoUso gestionarCampanaGruposTrxCasoUso;
    private final GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso;

    public ControladorDespliegues(
        GestionarDesplieguesCasoUso gestionarDesplieguesCasoUso,
        ConsultarEstadoCampanaFarmaciaCasoUso consultarEstadoCampanaFarmaciaCasoUso,
        GestionarCampanaGruposTrxCasoUso gestionarCampanaGruposTrxCasoUso,
        GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso
    ) {
        this.gestionarDesplieguesCasoUso = gestionarDesplieguesCasoUso;
        this.consultarEstadoCampanaFarmaciaCasoUso = consultarEstadoCampanaFarmaciaCasoUso;
        this.gestionarCampanaGruposTrxCasoUso = gestionarCampanaGruposTrxCasoUso;
        this.gestionarAuditoriaCasoUso = gestionarAuditoriaCasoUso;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaDespliegue crear(
        @Valid @RequestBody SolicitudCrearDespliegue solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        RespuestaDespliegue respuesta = aRespuesta(gestionarDesplieguesCasoUso.crear(new DatosCrearDespliegue(
            solicitud.idPaquete(),
            solicitud.nombre(),
            solicitud.descripcion(),
            solicitud.programadoEn(),
            solicitud.grupoObjetivo(),
            solicitud.piloto(),
            solicitud.idsEquipos()
        )));
        auditar(autenticacion, request, "DEPLOYMENT_CREATED", respuesta.id(), Map.of(
            "name", respuesta.nombre(),
            "status", respuesta.estado(),
            "packageId", respuesta.idPaquete(),
            "targetCount", respuesta.cantidadObjetivos()
        ));
        int farmaciasTurno = gestionarDesplieguesCasoUso.contarFarmaciasTurno(respuesta.id());
        if (farmaciasTurno > 0) {
            auditar(autenticacion, request, "ADVERTENCIA_CAMPANA_CON_TURNO", respuesta.id(), Map.of(
                "name", respuesta.nombre(),
                "onDutyBranches", farmaciasTurno,
                "message", "La campana incluye " + farmaciasTurno + " farmacias de turno. Revise impacto antes de continuar."
            ));
        }
        return respuesta;
    }

    @GetMapping
    public List<RespuestaDespliegue> listar() {
        return gestionarDesplieguesCasoUso.listar()
            .stream()
            .map(this::aRespuesta)
            .toList();
    }

    @GetMapping("/page")
    public RespuestaPagina<RespuestaDespliegue> listarPaginado(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String packageVersion,
        @RequestParam(required = false) OffsetDateTime createdFrom,
        @RequestParam(required = false) OffsetDateTime createdTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(defaultValue = "creadoEn,desc") String sort
    ) {
        Pagina<Despliegue> pagina = gestionarDesplieguesCasoUso.listarPaginado(new FiltroDespliegues(
            q,
            status,
            packageVersion,
            createdFrom,
            createdTo,
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
    public RespuestaDespliegue obtener(@PathVariable UUID id) {
        return aRespuesta(gestionarDesplieguesCasoUso.obtener(id));
    }

    @PostMapping({"/{id}/schedule", "/{id}/programar"})
    public RespuestaDespliegue programar(
        @PathVariable UUID id,
        @Valid @RequestBody SolicitudProgramarDespliegue solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        RespuestaDespliegue respuesta = aRespuesta(gestionarDesplieguesCasoUso.programar(id, solicitud.programadoEn()));
        auditarCambioEstado(autenticacion, request, "DEPLOYMENT_SCHEDULED", respuesta);
        return respuesta;
    }

    @PostMapping({"/{id}/pause", "/{id}/pausar"})
    public RespuestaDespliegue pausar(@PathVariable UUID id, Authentication autenticacion, HttpServletRequest request) {
        exigirOperador(autenticacion);
        RespuestaDespliegue respuesta = aRespuesta(gestionarDesplieguesCasoUso.pausar(id));
        auditarCambioEstado(autenticacion, request, "DEPLOYMENT_PAUSED", respuesta);
        return respuesta;
    }

    @PostMapping({"/{id}/resume", "/{id}/reanudar"})
    public RespuestaDespliegue reanudar(@PathVariable UUID id, Authentication autenticacion, HttpServletRequest request) {
        exigirOperador(autenticacion);
        RespuestaDespliegue respuesta = aRespuesta(gestionarDesplieguesCasoUso.reanudar(id));
        auditarCambioEstado(autenticacion, request, "DEPLOYMENT_RESUMED", respuesta);
        return respuesta;
    }

    @PostMapping({"/{id}/cancel", "/{id}/cancelar"})
    public RespuestaDespliegue cancelar(@PathVariable UUID id, Authentication autenticacion, HttpServletRequest request) {
        exigirOperador(autenticacion);
        RespuestaDespliegue respuesta = aRespuesta(gestionarDesplieguesCasoUso.cancelar(id));
        auditarCambioEstado(autenticacion, request, "DEPLOYMENT_CANCELLED", respuesta);
        return respuesta;
    }

    @GetMapping({"/{id}/status", "/{id}/estado-por-equipo"})
    public RespuestaEstadoDespliegue estado(@PathVariable UUID id) {
        return aRespuestaEstado(gestionarDesplieguesCasoUso.estado(id));
    }

    @GetMapping("/{id}/estado-por-farmacia")
    public RespuestaResumenEstadoCampanaFarmacia estadoPorFarmacia(
        @PathVariable UUID id,
        @RequestParam(required = false) String estadoTecnico,
        @RequestParam(required = false) String estadoOperacional,
        @RequestParam(required = false) String grupoTrx,
        @RequestParam(required = false) Boolean deTurno,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(defaultValue = "prioridad,asc") String sort
    ) {
        return aRespuestaResumen(consultarEstadoCampanaFarmaciaCasoUso.consultar(id, new FiltroEstadoCampanaFarmacia(
            estadoTecnico,
            estadoOperacional,
            grupoTrx,
            deTurno,
            q,
            page,
            size,
            sort
        )));
    }

    @GetMapping("/{id}/estado-por-trx")
    public RespuestaResumenCampanaGruposTrx estadoPorTrx(@PathVariable UUID id) {
        return aRespuestaResumenTrx(gestionarCampanaGruposTrxCasoUso.estadoPorTrx(id));
    }

    @PostMapping("/{id}/grupos-trx/{grupoTrxId}")
    public RespuestaCampanaGrupoTrx asociarGrupoTrx(
        @PathVariable UUID id,
        @PathVariable UUID grupoTrxId,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        CampanaGrupoTrx grupo = gestionarCampanaGruposTrxCasoUso.asociar(id, grupoTrxId);
        auditarGrupoTrxCampana(autenticacion, request, "ASOCIAR_GRUPO_TRX_CAMPANA", grupo, null, valoresGrupoCampana(grupo, null));
        if (grupo.farmaciasTurnoAfectadas() > 0) {
            auditarGrupoTrxCampana(autenticacion, request, "ADVERTENCIA_TRX_CON_FARMACIA_TURNO", grupo, null, valoresGrupoCampana(grupo, "Grupo TRX con farmacias de turno afectadas"));
        }
        return aRespuestaGrupoTrx(grupo);
    }

    @DeleteMapping("/{id}/grupos-trx/{grupoTrxId}")
    public void quitarGrupoTrx(
        @PathVariable UUID id,
        @PathVariable UUID grupoTrxId,
        @RequestBody(required = false) SolicitudMotivoOperacion solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        CampanaGrupoTrx grupo = gestionarCampanaGruposTrxCasoUso.estadoPorTrx(id).grupos().stream()
            .filter(item -> item.grupoTrxId().equals(grupoTrxId))
            .findFirst()
            .orElse(null);
        gestionarCampanaGruposTrxCasoUso.quitar(id, grupoTrxId);
        if (grupo != null) {
            auditarGrupoTrxCampana(autenticacion, request, "QUITAR_GRUPO_TRX_CAMPANA", grupo, valoresGrupoCampana(grupo, motivo(solicitud)), null);
        }
    }

    @PostMapping("/{id}/grupos-trx/{grupoTrxId}/pausar")
    public RespuestaCampanaGrupoTrx pausarGrupoTrxCampana(
        @PathVariable UUID id,
        @PathVariable UUID grupoTrxId,
        @RequestBody(required = false) SolicitudMotivoOperacion solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        CampanaGrupoTrx anterior = obtenerGrupoTrxCampana(id, grupoTrxId);
        CampanaGrupoTrx grupo = gestionarCampanaGruposTrxCasoUso.pausar(id, grupoTrxId, motivo(solicitud));
        auditarGrupoTrxCampana(autenticacion, request, "PAUSAR_GRUPO_TRX_CAMPANA", grupo, valoresGrupoCampana(anterior, motivo(solicitud)), valoresGrupoCampana(grupo, motivo(solicitud)));
        return aRespuestaGrupoTrx(grupo);
    }

    @PostMapping("/{id}/grupos-trx/{grupoTrxId}/reanudar")
    public RespuestaCampanaGrupoTrx reanudarGrupoTrxCampana(
        @PathVariable UUID id,
        @PathVariable UUID grupoTrxId,
        @RequestBody(required = false) SolicitudMotivoOperacion solicitud,
        Authentication autenticacion,
        HttpServletRequest request
    ) {
        exigirOperador(autenticacion);
        CampanaGrupoTrx anterior = obtenerGrupoTrxCampana(id, grupoTrxId);
        CampanaGrupoTrx grupo = gestionarCampanaGruposTrxCasoUso.reanudar(id, grupoTrxId);
        auditarGrupoTrxCampana(autenticacion, request, "REANUDAR_GRUPO_TRX_CAMPANA", grupo, valoresGrupoCampana(anterior, motivo(solicitud)), valoresGrupoCampana(grupo, motivo(solicitud)));
        return aRespuestaGrupoTrx(grupo);
    }

    private RespuestaDespliegue aRespuesta(Despliegue despliegue) {
        return new RespuestaDespliegue(
            despliegue.id(),
            despliegue.idPaquete(),
            despliegue.versionPaquete(),
            despliegue.nombre(),
            despliegue.descripcion(),
            despliegue.estado(),
            despliegue.programadoEn(),
            despliegue.creadoEn(),
            despliegue.cantidadObjetivos()
        );
    }

    private RespuestaEstadoDespliegue aRespuestaEstado(EstadoDespliegue estado) {
        return new RespuestaEstadoDespliegue(
            estado.idDespliegue(),
            estado.estado(),
            estado.totalObjetivos(),
            estado.objetivosCompletados(),
            estado.objetivosFallidos(),
            estado.objetivosPendientes(),
            estado.porcentajeAvance(),
            estado.porcentajeFallo(),
            estado.objetivosPorEstado()
        );
    }

    private RespuestaResumenEstadoCampanaFarmacia aRespuestaResumen(ResumenEstadoCampanaFarmacia resumen) {
        return new RespuestaResumenEstadoCampanaFarmacia(
            resumen.campanaId(),
            resumen.nombreCampana(),
            resumen.versionPos(),
            resumen.estadoCampana(),
            resumen.totalFarmacias(),
            resumen.farmaciasCompletadas(),
            resumen.farmaciasPendientes(),
            resumen.farmaciasEnProgreso(),
            resumen.farmaciasConErrores(),
            resumen.farmaciasEnRiesgo(),
            resumen.farmaciasCriticas(),
            resumen.farmaciasTurnoEnRiesgo(),
            resumen.avancePorcentaje(),
            resumen.exitoPorcentaje(),
            resumen.grupoTrxPeorEstado(),
            resumen.pagina(),
            resumen.tamano(),
            resumen.totalElementos(),
            resumen.totalPaginas(),
            resumen.tieneSiguiente(),
            resumen.farmacias().stream().map(this::aRespuestaEstadoFarmacia).toList()
        );
    }

    private RespuestaResumenCampanaGruposTrx aRespuestaResumenTrx(ResumenCampanaGruposTrx resumen) {
        return new RespuestaResumenCampanaGruposTrx(
            resumen.campanaId(),
            resumen.nombreCampana(),
            resumen.versionPos(),
            resumen.estadoCampana(),
            resumen.totalGrupos(),
            resumen.gruposEnRiesgo(),
            resumen.gruposPausados(),
            resumen.farmaciasAfectadas(),
            resumen.farmaciasTurnoAfectadas(),
            resumen.farmaciasCriticas(),
            resumen.grupos().stream().map(this::aRespuestaGrupoTrx).toList()
        );
    }

    private RespuestaCampanaGrupoTrx aRespuestaGrupoTrx(CampanaGrupoTrx grupo) {
        return new RespuestaCampanaGrupoTrx(
            grupo.id(),
            grupo.campanaId(),
            grupo.nombreCampana(),
            grupo.versionPos(),
            grupo.estadoCampana(),
            grupo.grupoTrxId(),
            grupo.codigoGrupoTrx(),
            grupo.nombreGrupoTrx(),
            grupo.orden(),
            grupo.estado().name(),
            grupo.totalFarmacias(),
            grupo.farmaciasAfectadas(),
            grupo.farmaciasTurnoAfectadas(),
            grupo.farmaciasCriticas(),
            grupo.farmaciasPendientes(),
            grupo.farmaciasConFallos(),
            grupo.equiposPosTotales(),
            grupo.equiposPosCompletados(),
            grupo.equiposPosPendientes(),
            grupo.equiposPosFallidos(),
            grupo.rollbacks(),
            grupo.motivoPausa(),
            grupo.resumenRiesgo(),
            grupo.iniciadoEn(),
            grupo.finalizadoEn(),
            grupo.creadoEn(),
            grupo.actualizadoEn(),
            grupo.farmacias().stream().map(this::aRespuestaEstadoFarmacia).toList()
        );
    }

    private RespuestaEstadoCampanaFarmacia aRespuestaEstadoFarmacia(EstadoCampanaFarmacia estado) {
        return new RespuestaEstadoCampanaFarmacia(
            estado.farmaciaId(),
            estado.codigoFarmacia(),
            estado.nombreFarmacia(),
            estado.campanaId(),
            estado.grupoTrxId(),
            estado.codigoGrupoTrx(),
            estado.deTurno(),
            estado.totalEquiposPos(),
            estado.completados(),
            estado.pendientes(),
            estado.fallidos(),
            estado.rollbacks(),
            estado.ultimoHeartbeatRelacionado(),
            estado.alertasCriticas(),
            estado.alertasAbiertas(),
            estado.estadoTecnico().name(),
            estado.estadoOperacional().name(),
            estado.resumenRiesgo(),
            estado.equipos().stream().map(this::aRespuestaEquipoEstadoFarmacia).toList()
        );
    }

    private RespuestaEquipoEstadoCampanaFarmacia aRespuestaEquipoEstadoFarmacia(EquipoEstadoCampanaFarmacia equipo) {
        return new RespuestaEquipoEstadoCampanaFarmacia(
            equipo.equipoId(),
            equipo.nombreEquipo(),
            equipo.estadoEquipo(),
            equipo.estadoObjetivo(),
            equipo.codigoGrupoTrx(),
            equipo.versionAnterior(),
            equipo.versionNueva(),
            equipo.ultimoError(),
            equipo.ultimoHeartbeatEn(),
            equipo.rollback()
        );
    }

    private void auditarCambioEstado(
        Authentication autenticacion,
        HttpServletRequest request,
        String accion,
        RespuestaDespliegue despliegue
    ) {
        auditar(autenticacion, request, accion, despliegue.id(), Map.of(
            "name", despliegue.nombre(),
            "status", despliegue.estado()
        ));
    }

    private void auditar(
        Authentication autenticacion,
        HttpServletRequest request,
        String accion,
        UUID idEntidad,
        Map<String, Object> valores
    ) {
        gestionarAuditoriaCasoUso.registrar(new DatosAuditoria(
            usuario(autenticacion),
            accion,
            "DEPLOYMENT",
            idEntidad,
            null,
            valores,
            direccionIp(request)
        ));
    }

    private void auditarGrupoTrxCampana(
        Authentication autenticacion,
        HttpServletRequest request,
        String accion,
        CampanaGrupoTrx grupo,
        Map<String, Object> valoresAnteriores,
        Map<String, Object> valoresNuevos
    ) {
        gestionarAuditoriaCasoUso.registrar(new DatosAuditoria(
            usuario(autenticacion),
            accion,
            "CAMPANA_GRUPO_TRX",
            grupo.campanaId(),
            valoresAnteriores,
            valoresNuevos,
            direccionIp(request)
        ));
    }

    private Map<String, Object> valoresGrupoCampana(CampanaGrupoTrx grupo, String motivo) {
        return Map.of(
            "campanaId", grupo.campanaId(),
            "grupoTrxId", grupo.grupoTrxId(),
            "codigoGrupoTrx", grupo.codigoGrupoTrx(),
            "estado", grupo.estado().name(),
            "farmaciasAfectadas", grupo.farmaciasAfectadas(),
            "farmaciasTurnoAfectadas", grupo.farmaciasTurnoAfectadas(),
            "farmaciasCriticas", grupo.farmaciasCriticas(),
            "motivo", motivo == null ? "" : motivo
        );
    }

    private CampanaGrupoTrx obtenerGrupoTrxCampana(UUID idCampana, UUID idGrupoTrx) {
        return gestionarCampanaGruposTrxCasoUso.estadoPorTrx(idCampana).grupos().stream()
            .filter(grupo -> grupo.grupoTrxId().equals(idGrupoTrx))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Grupo TRX no asociado a la campana POS."));
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
            "Solo ADMIN u OPERATOR pueden operar despliegues.",
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
