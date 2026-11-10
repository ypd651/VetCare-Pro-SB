package com.veterinaria.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mascotas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(nullable = false, length = 100)
    @ToString.Include
    private String nombre;

    @Column(nullable = false, length = 50)
    @ToString.Include
    private String especie;

    @Column(length = 80)
    private String raza;

    @Column(length = 20)
    private String sexo;

    private Integer edadAnios;

    private Double pesoKg;

    @Column(length = 50)
    private String color;

    @Column(length = 60, unique = true)
    private String microchip;

    @Builder.Default
    private Boolean esterilizado = false;

    @Column(length = 300)
    private String alergias;

    @Column(length = 600)
    private String notasMedicas;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fechaRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_id", nullable = false)
    private Propietario propietario;

    @OneToMany(mappedBy = "mascota", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Cita> citas = new ArrayList<>();

    @OneToMany(mappedBy = "mascota", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ConsultaMedica> consultas = new ArrayList<>();

    @OneToMany(mappedBy = "mascota", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Vacuna> vacunas = new ArrayList<>();
}
