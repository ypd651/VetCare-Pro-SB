package com.veterinaria.dto;

import com.veterinaria.entities.TipoInmunizacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacunaRequest {

    @NotNull(message = "Debe seleccionar una mascota")
    private Long mascotaId;

    @NotBlank(message = "El nombre de la vacuna o desparasitante es obligatorio")
    @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
    private String nombre;

    private TipoInmunizacion tipo;

    @NotNull(message = "La fecha de aplicación es obligatoria")
    private LocalDate fechaAplicacion;

    private LocalDate proximaDosis;

    @Size(max = 60, message = "El lote no puede exceder 60 caracteres")
    private String lote;

    @Size(max = 100, message = "El veterinario no puede exceder 100 caracteres")
    private String veterinario;

    @Size(max = 400, message = "Las notas no pueden exceder 400 caracteres")
    private String notas;
}
