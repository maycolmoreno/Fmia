package com.farmamia.posupdate.infraestructura.persistencia.repositorio;

import com.farmamia.posupdate.infraestructura.persistencia.entidad.ObjetivoDespliegueEntidad;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObjetivoDespliegueRepositorioJpa extends JpaRepository<ObjetivoDespliegueEntidad, UUID> {

    @Override
    @EntityGraph(attributePaths = {"despliegue", "despliegue.paquete", "equipo", "equipo.sucursal", "oleada", "grupoTrx"})
    List<ObjetivoDespliegueEntidad> findAll();

    @EntityGraph(attributePaths = {"despliegue", "despliegue.paquete", "oleada", "grupoTrx"})
    Optional<ObjetivoDespliegueEntidad> findFirstByEquipo_IdAndEstadoOrderByActualizadoEnDesc(
        UUID idEquipo,
        String estado
    );

    @EntityGraph(attributePaths = {"despliegue", "despliegue.paquete", "grupoTrx"})
    Optional<ObjetivoDespliegueEntidad> findByIdAndEquipo_Id(UUID id, UUID idEquipo);

    long countByDespliegue_Id(UUID idDespliegue);

    long countByDespliegue_IdAndEstadoIn(UUID idDespliegue, List<String> estados);

    @EntityGraph(attributePaths = {"equipo", "equipo.sucursal", "oleada", "grupoTrx"})
    List<ObjetivoDespliegueEntidad> findByDespliegue_Id(UUID idDespliegue);

    @EntityGraph(attributePaths = {"equipo", "equipo.sucursal", "oleada", "grupoTrx"})
    List<ObjetivoDespliegueEntidad> findByOleada_Id(UUID idOleada);

    long countByOleada_IdAndLeaseInstruccionHastaAfter(UUID idOleada, OffsetDateTime ahora);

    long countByLeaseInstruccionHastaBefore(OffsetDateTime ahora);

    @EntityGraph(attributePaths = {"despliegue", "despliegue.paquete", "grupoTrx"})
    List<ObjetivoDespliegueEntidad> findByEquipo_IdOrderByActualizadoEnDesc(UUID idEquipo);

    @Query("""
        select distinct o.equipo.sucursal.id
        from ObjetivoDespliegueEntidad o
        where o.equipo.sucursal.id in :idsSucursal
          and o.estado not in :estadosFinales
        """)
    List<UUID> buscarSucursalesConObjetivoActivo(
        @Param("idsSucursal") List<UUID> idsSucursal,
        @Param("estadosFinales") List<String> estadosFinales
    );
}
