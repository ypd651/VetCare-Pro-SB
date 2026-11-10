package com.veterinaria.repository;

import com.veterinaria.entities.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    @Query("SELECT m FROM Mascota m JOIN FETCH m.propietario ORDER BY m.id DESC")
    List<Mascota> findAllWithPropietario();

    @Query("SELECT m FROM Mascota m JOIN FETCH m.propietario WHERE m.id = :id")
    Optional<Mascota> findByIdWithPropietario(@Param("id") Long id);

    @Query("SELECT m FROM Mascota m JOIN FETCH m.propietario WHERE m.propietario.id = :propietarioId ORDER BY m.id DESC")
    List<Mascota> findByPropietarioIdWithPropietario(@Param("propietarioId") Long propietarioId);

    boolean existsByMicrochipIgnoreCase(String microchip);

    boolean existsByMicrochipIgnoreCaseAndIdNot(String microchip, Long id);

    @Query("SELECT m.especie, COUNT(m) FROM Mascota m GROUP BY m.especie ORDER BY COUNT(m) DESC")
    List<Object[]> countByEspecie();
}
