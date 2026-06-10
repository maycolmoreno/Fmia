package com.farmamia.operations.presentacion.controlador;

import com.farmamia.operations.aplicacion.casouso.ConsultarCatalogoOperativoCasoUso;
import com.farmamia.operations.dominio.modelo.Sucursal;
import com.farmamia.operations.presentacion.dto.RespuestaSucursal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/branches")
public class ControladorSucursales {

    private final ConsultarCatalogoOperativoCasoUso consultarCatalogoOperativoCasoUso;

    public ControladorSucursales(ConsultarCatalogoOperativoCasoUso consultarCatalogoOperativoCasoUso) {
        this.consultarCatalogoOperativoCasoUso = consultarCatalogoOperativoCasoUso;
    }

    @GetMapping
    public List<RespuestaSucursal> listar() {
        return consultarCatalogoOperativoCasoUso.listarSucursales()
            .stream()
            .map(this::aRespuesta)
            .toList();
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
}
