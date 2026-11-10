package com.veterinaria.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "vacunas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Vacuna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mascota_id", nullable = false)
    private Mascota mascota;

    @Column(nullable = false, length = 120)
    @ToString.Include
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private TipoInmunizacion tipo = TipoInmunizacion.VACUNA;

    @Column(nullable = false)
    private LocalDate fechaAplicacion;

    private LocalDate proximaDosis;

    @Column(length = 60)
    private String lote;

    @Column(length = 100)
    private String veterinario;

    @Column(length = 400)
    private String notas;
}
