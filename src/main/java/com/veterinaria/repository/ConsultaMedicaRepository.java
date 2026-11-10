package com.veterinaria.repository;

import com.veterinaria.entities.ConsultaMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultaMedicaRepository extends JpaRepository<ConsultaMedica, Long> {

    @Query("SELECT cm FROM ConsultaMedica cm JOIN FETCH cm.mascota m JOIN FETCH m.propietario LEFT JOIN FETCH cm.cita ORDER BY cm.fechaConsulta DESC")
    List<ConsultaMedica> findAllWithDetails();

    @Query("SELECT cm FROM ConsultaMedica cm JOIN FETCH cm.mascota m JOIN FETCH m.propietario LEFT JOIN FETCH cm.cita WHERE cm.id = :id")
    Optional<ConsultaMedica> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT cm FROM ConsultaMedica cm JOIN FETCH cm.mascota m JOIN FETCH m.propietario LEFT JOIN FETCH cm.cita WHERE m.id = :mascotaId ORDER BY cm.fechaConsulta DESC")
    List<ConsultaMedica> findByMascotaIdWithDetails(@Param("mascotaId") Long mascotaId);

    @Query("SELECT cm.mascota.id, COUNT(cm) FROM ConsultaMedica cm GROUP BY cm.mascota.id")
    List<Object[]> countGroupedByMascota();

    @Query("""
            SELECT cm FROM ConsultaMedica cm
            JOIN FETCH cm.mascota m
            JOIN FETCH m.propietario
            LEFT JOIN FETCH cm.cita
            WHERE cm.proximaRevision IS NOT NULL
              AND cm.proximaRevision BETWEEN :from AND :to
            ORDER BY cm.proximaRevision ASC
            """)
    List<ConsultaMedica> findUpcomingRevisionsWithDetails(@Param("from") LocalDate from, @Param("to") LocalDate to);

    long countByFechaConsultaAfter(LocalDateTime from);

    @Modifying
    @Query("UPDATE ConsultaMedica cm SET cm.cita = null WHERE cm.cita.id = :citaId")
    void clearCitaReferences(@Param("citaId") Long citaId);
}
