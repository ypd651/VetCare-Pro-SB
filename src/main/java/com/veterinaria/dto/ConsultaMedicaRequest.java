package com.veterinaria.dto;

import jakarta.validation.constraints.*;
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
public class ConsultaMedicaRequest {

    @NotNull(message = "Debe seleccionar una mascota")
    private Long mascotaId;

    private Long citaId;

    private LocalDateTime fechaConsulta;

    @NotBlank(message = "El nombre del veterinario es obligatorio")
    @Size(max = 100, message = "El nombre del veterinario no puede exceder 100 caracteres")
    private String veterinario;

    @NotBlank(message = "El motivo de la consulta es obligatorio")
    @Size(max = 250, message = "El motivo no puede exceder 250 caracteres")
    private String motivoConsulta;

    @Size(max = 500, message = "Los síntomas no pueden exceder 500 caracteres")
    private String sintomas;

    @NotBlank(message = "El diagnóstico es obligatorio")
    @Size(max = 600, message = "El diagnóstico no puede exceder 600 caracteres")
    private String diagnostico;

    @NotBlank(message = "El tratamiento es obligatorio")
    @Size(max = 800, message = "El tratamiento no puede exceder 800 caracteres")
    private String tratamiento;

    @DecimalMin(value = "0.0", message = "El peso no puede ser negativo")
    @DecimalMax(value = "1500.0", message = "El peso supera el máximo permitido")
    private Double pesoKg;

    @DecimalMin(value = "25.0", message = "La temperatura mínima válida es 25.0 °C")
    @DecimalMax(value = "45.0", message = "La temperatura máxima válida es 45.0 °C")
    private Double temperaturaC;

    @Min(value = 10, message = "La frecuencia cardíaca mínima es 10 lpm")
    @Max(value = 400, message = "La frecuencia cardíaca máxima es 400 lpm")
    private Integer frecuenciaCardiaca;

    private LocalDate proximaRevision;

    @Size(max = 600, message = "Las observaciones no pueden exceder 600 caracteres")
    private String observaciones;
}
