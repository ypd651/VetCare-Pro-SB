package com.veterinaria.dto;

import com.veterinaria.entities.EstadoCita;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitaEstadoRequest {
    @NotNull(message = "El estado de la cita es obligatorio")
    private EstadoCita estado;
}
