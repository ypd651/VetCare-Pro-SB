package com.veterinaria.repository;

import com.veterinaria.entities.Vacuna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VacunaRepository extends JpaRepository<Vacuna, Long> {

    @Query("""
            SELECT v FROM Vacuna v
            JOIN FETCH v.mascota m
            JOIN FETCH m.propietario
            ORDER BY v.proximaDosis ASC NULLS LAST, v.fechaAplicacion DESC
            """)
    List<Vacuna> findAllWithDetails();

    @Query("SELECT v FROM Vacuna v JOIN FETCH v.mascota m JOIN FETCH m.propietario WHERE v.id = :id")
    Optional<Vacuna> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT v FROM Vacuna v
            JOIN FETCH v.mascota m
            JOIN FETCH m.propietario
            WHERE m.id = :mascotaId
            ORDER BY v.proximaDosis ASC NULLS LAST, v.fechaAplicacion DESC
            """)
    List<Vacuna> findByMascotaIdWithDetails(@Param("mascotaId") Long mascotaId);

    @Query("""
            SELECT v FROM Vacuna v
            JOIN FETCH v.mascota m
            JOIN FETCH m.propietario
            WHERE v.proximaDosis IS NOT NULL
              AND v.proximaDosis <= :limitDate
            ORDER BY v.proximaDosis ASC
            """)
    List<Vacuna> findAlertasUntilWithDetails(@Param("limitDate") LocalDate limitDate);
}
