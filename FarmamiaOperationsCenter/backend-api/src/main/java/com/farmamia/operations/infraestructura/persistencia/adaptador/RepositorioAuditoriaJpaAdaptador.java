package com.farmamia.operations.infraestructura.persistencia.adaptador;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.farmamia.operations.dominio.modelo.AuditoriaRegistrada;
import com.farmamia.operations.dominio.modelo.DatosAuditoria;
import com.farmamia.operations.dominio.modelo.FiltroAuditoria;
import com.farmamia.operations.dominio.modelo.FiltroAuditoriaPaginada;
import com.farmamia.operations.dominio.modelo.Pagina;
import com.farmamia.operations.dominio.puerto.RepositorioAuditoria;
import com.farmamia.operations.infraestructura.persistencia.entidad.AuditoriaEntidad;
import com.farmamia.operations.infraestructura.persistencia.entidad.UsuarioAppEntidad;
import com.farmamia.operations.infraestructura.persistencia.repositorio.AuditoriaRepositorioJpa;
import com.farmamia.operations.infraestructura.persistencia.repositorio.UsuarioAppRepositorioJpa;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioAuditoriaJpaAdaptador implements RepositorioAuditoria {

    private final AuditoriaRepositorioJpa auditoriaRepositorioJpa;
    private final UsuarioAppRepositorioJpa usuarioAppRepositorioJpa;
    private final ObjectMapper objectMapper;

    public RepositorioAuditoriaJpaAdaptador(
        AuditoriaRepositorioJpa auditoriaRepositorioJpa,
        UsuarioAppRepositorioJpa usuarioAppRepositorioJpa,
        ObjectMapper objectMapper
    ) {
        this.auditoriaRepositorioJpa = auditoriaRepositorioJpa;
        this.usuarioAppRepositorioJpa = usuarioAppRepositorioJpa;
        this.objectMapper = objectMapper;
    }

    @Override
    public void registrar(DatosAuditoria datos) {
        UsuarioAppEntidad usuario = datos.usuarioActor() == null || datos.usuarioActor().isBlank()
            ? null
            : usuarioAppRepositorioJpa.findByUsuario(datos.usuarioActor()).orElse(null);

        auditoriaRepositorioJpa.save(new AuditoriaEntidad(
            usuario,
            datos.accion(),
            datos.tipoEntidad(),
            datos.idEntidad(),
            aJson(datos.valoresAnteriores()),
            aJson(datos.valoresNuevos()),
            datos.direccionIp()
        ));
    }

    @Override
    public List<AuditoriaRegistrada> listarRecientes(int limite) {
        return auditoriaRepositorioJpa.findByOrderByCreadoEnDesc(PageRequest.of(0, limite))
            .stream()
            .map(this::aDominio)
            .toList();
    }

    @Override
    public List<AuditoriaRegistrada> listarConFiltros(FiltroAuditoria filtro) {
        return auditoriaRepositorioJpa.buscarConFiltros(
            minusculaANulo(filtro.accion()),
            minusculaANulo(filtro.tipoEntidad()),
            minusculaANulo(filtro.usuarioActor()),
            filtro.desde(),
            filtro.hasta(),
            PageRequest.of(0, filtro.limite())
        )
            .stream()
            .map(this::aDominio)
            .toList();
    }

    @Override
    public Pagina<AuditoriaRegistrada> listarPaginado(FiltroAuditoriaPaginada filtro) {
        org.springframework.data.domain.Page<AuditoriaEntidad> pagina = auditoriaRepositorioJpa.buscarConFiltrosPaginado(
            minusculaANulo(filtro.accion()),
            minusculaANulo(filtro.tipoEntidad()),
            minusculaANulo(filtro.usuarioActor()),
            filtro.desde(),
            filtro.hasta(),
            PageRequest.of(filtro.pagina(), filtro.tamano(), aOrden(filtro.orden()))
        );

        return new Pagina<>(
            pagina.getContent().stream().map(this::aDominio).toList(),
            pagina.getNumber(),
            pagina.getSize(),
            pagina.getTotalElements(),
            pagina.getTotalPages(),
            pagina.hasNext()
        );
    }

    private AuditoriaRegistrada aDominio(AuditoriaEntidad entidad) {
        UsuarioAppEntidad usuario = entidad.getUsuarioActor();
        return new AuditoriaRegistrada(
            entidad.getId(),
            usuario == null ? null : usuario.getId(),
            usuario == null ? null : usuario.getUsuario(),
            entidad.getAccion(),
            entidad.getTipoEntidad(),
            entidad.getIdEntidad(),
            aMapa(entidad.getValoresAnterioresJson()),
            aMapa(entidad.getValoresNuevosJson()),
            entidad.getDireccionIp(),
            entidad.getCreadoEn()
        );
    }

    private String aJson(Map<String, Object> valores) {
        if (valores == null || valores.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(valores);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Valores de auditoria invalidos", ex);
        }
    }

    private Map<String, Object> aMapa(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return Map.of("raw", json);
        }
    }

    private Sort aOrden(String orden) {
        String[] partes = orden == null ? new String[0] : orden.split(",", 2);
        String campo = partes.length > 0 ? partes[0] : "creadoEn";
        Sort.Direction direccion = partes.length > 1 && "asc".equalsIgnoreCase(partes[1])
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        return Sort.by(direccion, switch (campo) {
            case "action", "accion" -> "accion";
            case "entityType", "tipoEntidad" -> "tipoEntidad";
            default -> "creadoEn";
        });
    }

    private String minusculaANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase(Locale.ROOT);
    }
}
