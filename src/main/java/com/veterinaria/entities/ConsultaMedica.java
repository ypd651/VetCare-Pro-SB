package com.veterinaria.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultas_medicas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class ConsultaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mascota_id", nullable = false)
    private Mascota mascota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    @Column(nullable = false)
    @ToString.Include
    private LocalDateTime fechaConsulta;

    @Column(nullable = false, length = 100)
    private String veterinario;

    @Column(nullable = false, length = 250)
    private String motivoConsulta;

    @Column(length = 500)
    private String sintomas;

    @Column(nullable = false, length = 600)
    private String diagnostico;

    @Column(nullable = false, length = 800)
    private String tratamiento;

    private Double pesoKg;

    private Double temperaturaC;

    private Integer frecuenciaCardiaca;

    private LocalDate proximaRevision;

    @Column(length = 600)
    private String observaciones;
}
