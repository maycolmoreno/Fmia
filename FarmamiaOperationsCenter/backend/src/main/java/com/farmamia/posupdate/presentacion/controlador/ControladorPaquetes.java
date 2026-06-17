package com.farmamia.posupdate.presentacion.controlador;

import com.farmamia.posupdate.aplicacion.casouso.GestionarAuditoriaCasoUso;
import com.farmamia.posupdate.aplicacion.casouso.GestionarPaquetesPosCasoUso;
import com.farmamia.posupdate.dominio.modelo.DatosAuditoria;
import com.farmamia.posupdate.dominio.modelo.ArchivoPaqueteDescarga;
import com.farmamia.posupdate.dominio.modelo.FiltroPaquetesPos;
import com.farmamia.posupdate.dominio.modelo.Pagina;
import com.farmamia.posupdate.dominio.modelo.PaquetePos;
import com.farmamia.posupdate.infraestructura.observabilidad.MetricasOperativasFarmamia;
import com.farmamia.posupdate.presentacion.dto.RespuestaPaquetePos;
import com.farmamia.posupdate.presentacion.dto.RespuestaPagina;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/packages", "/api/versiones-pos"})
public class ControladorPaquetes {

    private final GestionarPaquetesPosCasoUso gestionarPaquetesPosCasoUso;
    private final GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso;
    private final MetricasOperativasFarmamia metricasOperativas;

    public ControladorPaquetes(
        GestionarPaquetesPosCasoUso gestionarPaquetesPosCasoUso,
        GestionarAuditoriaCasoUso gestionarAuditoriaCasoUso,
        MetricasOperativasFarmamia metricasOperativas
    ) {
        this.gestionarPaquetesPosCasoUso = gestionarPaquetesPosCasoUso;
        this.gestionarAuditoriaCasoUso = gestionarAuditoriaCasoUso;
        this.metricasOperativas = metricasOperativas;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RespuestaPaquetePos cargar(
        @RequestParam("version") String version,
        @RequestPart("file") MultipartFile archivo,
        Authentication autenticacion,
        HttpServletRequest request
    ) throws IOException {
        PermisosAdministrativos.exigirRol(
            autenticacion,
            "Solo ADMIN u OPERATOR pueden operar paquetes POS.",
            "ADMIN",
            "OPERATOR"
        );
        RespuestaPaquetePos respuesta = aRespuesta(gestionarPaquetesPosCasoUso.cargar(
            version,
            archivo.getOriginalFilename(),
            archivo.getInputStream()
        ));
        auditar(autenticacion, request, "PACKAGE_UPLOADED", respuesta.id(), Map.of(
            "version", respuesta.version(),
            "fileName", respuesta.nombreArchivo(),
            "status", respuesta.estado()
        ));
        return respuesta;
    }

    @GetMapping
    public List<RespuestaPaquetePos> listar() {
        return gestionarPaquetesPosCasoUso.listar()
            .stream()
            .map(this::aRespuesta)
            .toList();
    }

    @GetMapping("/page")
    public RespuestaPagina<RespuestaPaquetePos> listarPaginado(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String version,
        @RequestParam(required = false) OffsetDateTime uploadedFrom,
        @RequestParam(required = false) OffsetDateTime uploadedTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(defaultValue = "cargadoEn,desc") String sort
    ) {
        Pagina<PaquetePos> pagina = gestionarPaquetesPosCasoUso.listarPaginado(new FiltroPaquetesPos(
            q,
            status,
            version,
            uploadedFrom,
            uploadedTo,
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
    public RespuestaPaquetePos obtener(@PathVariable UUID id) {
        return aRespuesta(gestionarPaquetesPosCasoUso.obtener(id));
    }

    @PostMapping({"/{id}/approve", "/{id}/aprobar"})
    public RespuestaPaquetePos aprobar(@PathVariable UUID id, Authentication autenticacion, HttpServletRequest request) {
        PermisosAdministrativos.exigirRol(
            autenticacion,
            "Solo ADMIN u OPERATOR pueden aprobar paquetes POS.",
            "ADMIN",
            "OPERATOR"
        );
        RespuestaPaquetePos respuesta = aRespuesta(gestionarPaquetesPosCasoUso.aprobar(id));
        auditar(autenticacion, request, "PACKAGE_APPROVED", respuesta.id(), Map.of(
            "version", respuesta.version(),
            "status", respuesta.estado()
        ));
        return respuesta;
    }

    @PostMapping({"/{id}/retire", "/{id}/retirar"})
    public RespuestaPaquetePos retirar(@PathVariable UUID id, Authentication autenticacion, HttpServletRequest request) {
        PermisosAdministrativos.exigirRol(
            autenticacion,
            "Solo ADMIN u OPERATOR pueden retirar paquetes POS.",
            "ADMIN",
            "OPERATOR"
        );
        RespuestaPaquetePos respuesta = aRespuesta(gestionarPaquetesPosCasoUso.retirar(id));
        auditar(autenticacion, request, "PACKAGE_RETIRED", respuesta.id(), Map.of(
            "version", respuesta.version(),
            "status", respuesta.estado()
        ));
        return respuesta;
    }

    @GetMapping({"/{id}/download", "/{id}/descargar"})
    public ResponseEntity<Resource> descargar(@PathVariable UUID id) {
        ArchivoPaqueteDescarga paquete = gestionarPaquetesPosCasoUso.descargar(id);
        metricasOperativas.registrarDescargaPaquete();

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(paquete.nombreArchivo()).build().toString()
            )
            .body(new InputStreamResource(paquete.contenido()));
    }

    private RespuestaPaquetePos aRespuesta(PaquetePos paquete) {
        return new RespuestaPaquetePos(
            paquete.getId(),
            paquete.getVersion(),
            paquete.getNombreArchivo(),
            paquete.getChecksumSha256(),
            paquete.getFirma() == null ? null : paquete.getFirma().firma(),
            paquete.getFirma() == null ? null : paquete.getFirma().algoritmo(),
            paquete.getFirma() == null ? null : paquete.getFirma().idClave(),
            paquete.getFirma() == null ? "UNSIGNED" : paquete.getFirma().estado(),
            paquete.getTamanoBytes(),
            paquete.getEstado(),
            "/api/packages/" + paquete.getId() + "/download",
            paquete.getCargadoEn(),
            paquete.getAprobadoEn()
        );
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
            "POS_PACKAGE",
            idEntidad,
            null,
            valores,
            direccionIp(request)
        ));
    }

    private String usuario(Authentication autenticacion) {
        return autenticacion == null ? null : autenticacion.getName();
    }

    private String direccionIp(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
