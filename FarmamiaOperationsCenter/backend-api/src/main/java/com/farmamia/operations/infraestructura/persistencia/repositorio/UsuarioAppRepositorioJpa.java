package com.farmamia.operations.infraestructura.persistencia.repositorio;

import com.farmamia.operations.infraestructura.persistencia.entidad.UsuarioAppEntidad;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioAppRepositorioJpa extends JpaRepository<UsuarioAppEntidad, UUID> {

    Optional<UsuarioAppEntidad> findByUsuario(String usuario);

    boolean existsByUsuario(String usuario);

    boolean existsByCorreo(String correo);
}
