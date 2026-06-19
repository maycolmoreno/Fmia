package com.farmamia.posupdate.infraestructura.persistencia.repositorio;

import com.farmamia.posupdate.infraestructura.persistencia.entidad.MetricaEquipoEntidad;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MetricaEquipoRepositorioJpa extends JpaRepository<MetricaEquipoEntidad, UUID> {

    Optional<MetricaEquipoEntidad> findFirstByEquipo_IdOrderByRecolectadoEnDesc(UUID idEquipo);

    List<MetricaEquipoEntidad> findByEquipo_IdOrderByRecolectadoEnDesc(UUID idEquipo, Pageable pageable);

    @Modifying
    @Query("delete from MetricaEquipoEntidad metrica where metrica.recolectadoEn < :fechaCorte")
    int eliminarAnterioresA(@Param("fechaCorte") OffsetDateTime fechaCorte);
}
