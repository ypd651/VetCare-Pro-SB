package com.veterinaria.dto;

import com.veterinaria.entities.PrioridadCita;
import com.veterinaria.entities.TipoCita;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaRequest {

    @NotNull(message = "Debe seleccionar una mascota")
    private Long mascotaId;

    @NotNull(message = "La fecha y hora de la cita son obligatorias")
    private LocalDateTime fechaHora;

    @NotNull(message = "El tipo de cita es obligatorio")
    private TipoCita tipo;

    private PrioridadCita prioridad;

    @NotBlank(message = "El veterinario asignado es obligatorio")
    @Size(max = 100, message = "El nombre del veterinario no puede exceder 100 caracteres")
    private String veterinario;

    @NotBlank(message = "El motivo de la cita es obligatorio")
    @Size(max = 250, message = "El motivo no puede exceder 250 caracteres")
    private String motivo;

    @Size(max = 600, message = "Las notas no pueden exceder 600 caracteres")
    private String notas;
}
