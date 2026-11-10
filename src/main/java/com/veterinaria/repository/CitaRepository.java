package com.veterinaria.repository;

import com.veterinaria.entities.Cita;
import com.veterinaria.entities.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    @Query("SELECT c FROM Cita c JOIN FETCH c.mascota m JOIN FETCH m.propietario ORDER BY c.fechaHora ASC")
    List<Cita> findAllWithDetails();

    @Query("SELECT c FROM Cita c JOIN FETCH c.mascota m JOIN FETCH m.propietario WHERE c.id = :id")
    Optional<Cita> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT c FROM Cita c JOIN FETCH c.mascota m JOIN FETCH m.propietario WHERE m.id = :mascotaId ORDER BY c.fechaHora DESC")
    List<Cita> findByMascotaIdWithDetails(@Param("mascotaId") Long mascotaId);

    @Query("""
            SELECT c FROM Cita c
            JOIN FETCH c.mascota m
            JOIN FETCH m.propietario
            WHERE c.fechaHora BETWEEN :start AND :end
            ORDER BY c.fechaHora ASC
            """)
    List<Cita> findByFechaHoraBetweenWithDetails(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            SELECT c FROM Cita c
            JOIN FETCH c.mascota m
            WHERE LOWER(TRIM(c.veterinario)) = LOWER(TRIM(:veterinario))
              AND c.estado <> com.veterinaria.entities.EstadoCita.CANCELADA
              AND c.fechaHora BETWEEN :start AND :end
            """)
    List<Cita> findActiveConflictsForVet(
            @Param("veterinario") String veterinario,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            SELECT c FROM Cita c
            JOIN FETCH c.mascota m
            WHERE m.id = :mascotaId
              AND c.estado <> com.veterinaria.entities.EstadoCita.CANCELADA
              AND c.fechaHora BETWEEN :start AND :end
            """)
    List<Cita> findActiveConflictsForMascota(
            @Param("mascotaId") Long mascotaId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    long countByEstadoIn(List<EstadoCita> estados);
}
