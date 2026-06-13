package com.farmamia.operations.presentacion.controlador;

import com.farmamia.operations.aplicacion.casouso.ConsultarEstadoFarmaciasCasoUso;
import com.farmamia.operations.aplicacion.casouso.ConsultarCatalogoOperativoCasoUso;
import com.farmamia.operations.dominio.modelo.EstadoOperacionalFarmacia;
import com.farmamia.operations.dominio.modelo.FiltroSucursales;
import com.farmamia.operations.dominio.modelo.Pagina;
import com.farmamia.operations.dominio.modelo.Sucursal;
import com.farmamia.operations.presentacion.dto.RespuestaEstadoOperacionalFarmacia;
import com.farmamia.operations.presentacion.dto.RespuestaPagina;
import com.farmamia.operations.presentacion.dto.RespuestaSucursal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/branches", "/api/farmacias"})
public class ControladorSucursales {

    private final ConsultarCatalogoOperativoCasoUso consultarCatalogoOperativoCasoUso;
    private final ConsultarEstadoFarmaciasCasoUso consultarEstadoFarmaciasCasoUso;

    public ControladorSucursales(
        ConsultarCatalogoOperativoCasoUso consultarCatalogoOperativoCasoUso,
        ConsultarEstadoFarmaciasCasoUso consultarEstadoFarmaciasCasoUso
    ) {
        this.consultarCatalogoOperativoCasoUso = consultarCatalogoOperativoCasoUso;
        this.consultarEstadoFarmaciasCasoUso = consultarEstadoFarmaciasCasoUso;
    }

    @GetMapping("/estado")
    public List<RespuestaEstadoOperacionalFarmacia> listarEstadoOperacional() {
        return consultarEstadoFarmaciasCasoUso.listar()
            .stream()
            .map(this::aRespuestaEstado)
            .toList();
    }

    @GetMapping("/{id}/estado")
    public RespuestaEstadoOperacionalFarmacia obtenerEstadoOperacional(@PathVariable UUID id) {
        return aRespuestaEstado(consultarEstadoFarmaciasCasoUso.obtener(id));
    }

    @GetMapping
    public List<RespuestaSucursal> listar() {
        return consultarCatalogoOperativoCasoUso.listarSucursales()
            .stream()
            .map(this::aRespuesta)
            .toList();
    }

    @GetMapping("/page")
    public RespuestaPagina<RespuestaSucursal> listarPaginado(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String city,
        @RequestParam(required = false) String zone,
        @RequestParam(required = false) Boolean onDuty,
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(defaultValue = "code,asc") String sort
    ) {
        Pagina<Sucursal> pagina = consultarCatalogoOperativoCasoUso.listarSucursalesPaginado(new FiltroSucursales(
            q,
            code,
            city,
            zone,
            onDuty,
            active,
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

    private RespuestaSucursal aRespuesta(Sucursal sucursal) {
        return new RespuestaSucursal(
            sucursal.id(),
            sucursal.codigo(),
            sucursal.nombre(),
            sucursal.ciudad(),
            sucursal.zona(),
            sucursal.direccion(),
            sucursal.deTurno(),
            sucursal.activa(),
            sucursal.creadoEn(),
            sucursal.actualizadoEn()
        );
    }

    private RespuestaEstadoOperacionalFarmacia aRespuestaEstado(EstadoOperacionalFarmacia estado) {
        return new RespuestaEstadoOperacionalFarmacia(
            estado.idFarmacia(),
            estado.codigoFarmacia(),
            estado.nombreFarmacia(),
            estado.ciudad(),
            estado.zona(),
            estado.deTurno(),
            estado.activa(),
            estado.estadoOperacional(),
            estado.critica(),
            estado.turnoEnRiesgo(),
            estado.totalEquiposPos(),
            estado.equiposOnline(),
            estado.equiposOffline(),
            estado.equiposSinLatido(),
            estado.ultimoLatidoEn(),
            estado.alertasAbiertas(),
            estado.alertasCriticas(),
            estado.campanasActivas(),
            estado.objetivosCampanaPendientes(),
            estado.objetivosCampanaFallidos(),
            estado.campanaActivaPrincipal(),
            estado.grupoTrxPrincipal(),
            estado.versionPosDominante(),
            estado.resumenRiesgo()
        );
    }
}
