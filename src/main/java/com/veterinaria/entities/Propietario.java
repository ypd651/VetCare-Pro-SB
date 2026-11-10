package com.veterinaria.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "propietarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Propietario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(nullable = false, length = 120)
    @ToString.Include
    private String nombre;

    @Column(length = 40, unique = true)
    private String documento;

    @Column(nullable = false, length = 30)
    private String telefono;

    @Column(length = 120, unique = true)
    private String email;

    @Column(length = 200)
    private String direccion;

    @Column(length = 500)
    private String notas;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fechaRegistro;

    @OneToMany(mappedBy = "propietario", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Mascota> mascotas = new ArrayList<>();
}
