package com.veterinaria.dto;

import com.veterinaria.entities.EstadoCita;
import com.veterinaria.entities.PrioridadCita;
import com.veterinaria.entities.TipoCita;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaResponse {
    private Long id;
    private Long mascotaId;
    private String nombreMascota;
    private String especieMascota;
    private Long propietarioId;
    private String nombrePropietario;
    private String telefonoPropietario;
    private LocalDateTime fechaHora;
    private TipoCita tipo;
    private PrioridadCita prioridad;
    private EstadoCita estado;
    private String veterinario;
    private String motivo;
    private String notas;
    private LocalDateTime fechaCreacion;
}
