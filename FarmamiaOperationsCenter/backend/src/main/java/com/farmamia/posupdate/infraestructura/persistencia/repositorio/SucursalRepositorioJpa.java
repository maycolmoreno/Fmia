package com.farmamia.posupdate.infraestructura.persistencia.repositorio;

import com.farmamia.posupdate.infraestructura.persistencia.entidad.SucursalEntidad;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SucursalRepositorioJpa extends JpaRepository<SucursalEntidad, UUID> {

    Optional<SucursalEntidad> findByCodigo(String codigo);

    @Query("""
        select sucursal
        from SucursalEntidad sucursal
        where (:filtrarQ = false or (
            lower(sucursal.codigo) like concat('%', :q, '%')
            or lower(sucursal.nombre) like concat('%', :q, '%')
            or lower(coalesce(sucursal.ciudad, '')) like concat('%', :q, '%')
            or lower(coalesce(sucursal.zona, '')) like concat('%', :q, '%')
        ))
          and (:filtrarCodigo = false or lower(sucursal.codigo) = :codigo)
          and (:filtrarCiudad = false or lower(sucursal.ciudad) = :ciudad)
          and (:filtrarZona = false or lower(sucursal.zona) = :zona)
          and (:filtrarDeTurno = false or sucursal.deTurno = :deTurno)
          and (:filtrarActiva = false or sucursal.activa = :activa)
        """)
    Page<SucursalEntidad> buscarConFiltros(
        @Param("filtrarQ") boolean filtrarQ,
        @Param("q") String q,
        @Param("filtrarCodigo") boolean filtrarCodigo,
        @Param("codigo") String codigo,
        @Param("filtrarCiudad") boolean filtrarCiudad,
        @Param("ciudad") String ciudad,
        @Param("filtrarZona") boolean filtrarZona,
        @Param("zona") String zona,
        @Param("filtrarDeTurno") boolean filtrarDeTurno,
        @Param("deTurno") Boolean deTurno,
        @Param("filtrarActiva") boolean filtrarActiva,
        @Param("activa") Boolean activa,
        Pageable pageable
    );
}
