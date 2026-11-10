package com.veterinaria.dto;

import com.veterinaria.entities.EstadoVacuna;
import com.veterinaria.entities.TipoInmunizacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacunaResponse {
    private Long id;
    private Long mascotaId;
    private String nombreMascota;
    private String especieMascota;
    private Long propietarioId;
    private String nombrePropietario;
    private String telefonoPropietario;
    private String emailPropietario;
    private String nombre;
    private TipoInmunizacion tipo;
    private LocalDate fechaAplicacion;
    private LocalDate proximaDosis;
    private String lote;
    private String veterinario;
    private String notas;
    private EstadoVacuna estadoAlerta;
    private Long diasParaVencimiento;
}
