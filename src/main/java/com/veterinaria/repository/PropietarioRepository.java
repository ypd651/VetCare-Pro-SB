package com.veterinaria.repository;

import com.veterinaria.entities.Propietario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropietarioRepository extends JpaRepository<Propietario, Long> {

    @Query("SELECT DISTINCT p FROM Propietario p LEFT JOIN FETCH p.mascotas ORDER BY p.id DESC")
    List<Propietario> findAllWithMascotas();

    @Query("SELECT p FROM Propietario p LEFT JOIN FETCH p.mascotas WHERE p.id = :id")
    Optional<Propietario> findByIdWithMascotas(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT p FROM Propietario p
            LEFT JOIN FETCH p.mascotas
            WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(p.documento, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.telefono) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY p.id DESC
            """)
    List<Propietario> searchWithMascotas(@Param("q") String q);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByDocumentoIgnoreCase(String documento);

    boolean existsByDocumentoIgnoreCaseAndIdNot(String documento, Long id);
}
