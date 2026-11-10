package com.veterinaria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaMedicaResponse {
    private Long id;
    private Long mascotaId;
    private String nombreMascota;
    private String especieMascota;
    private String nombrePropietario;
    private String telefonoPropietario;
    private Long citaId;
    private LocalDateTime fechaConsulta;
    private String veterinario;
    private String motivoConsulta;
    private String sintomas;
    private String diagnostico;
    private String tratamiento;
    private Double pesoKg;
    private Double temperaturaC;
    private Integer frecuenciaCardiaca;
    private LocalDate proximaRevision;
    private String observaciones;
}
