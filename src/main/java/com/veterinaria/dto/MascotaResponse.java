package com.veterinaria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MascotaResponse {
    private Long id;
    private String nombre;
    private String especie;
    private String raza;
    private String sexo;
    private Integer edadAnios;
    private Double pesoKg;
    private String color;
    private String microchip;
    private Boolean esterilizado;
    private String alergias;
    private String notasMedicas;
    private LocalDateTime fechaRegistro;
    private Long propietarioId;
    private String nombrePropietario;
    private String telefonoPropietario;
    private long totalConsultas;
    private long alertasVacunas;
}
