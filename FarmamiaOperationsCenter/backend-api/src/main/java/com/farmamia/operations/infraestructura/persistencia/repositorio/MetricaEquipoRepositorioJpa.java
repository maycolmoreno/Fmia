package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.MetricaEquipoEntidad;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MetricaEquipoRepositorioJpa extends JpaRepository<MetricaEquipoEntidad, UUID> {

    Optional<MetricaEquipoEntidad> findFirstByEquipo_IdOrderByRecolectadoEnDesc(UUID idEquipo);

    @Modifying
    @Query("delete from MetricaEquipoEntidad metrica where metrica.recolectadoEn < :fechaCorte")
    int eliminarAnterioresA(@Param("fechaCorte") OffsetDateTime fechaCorte);
}
