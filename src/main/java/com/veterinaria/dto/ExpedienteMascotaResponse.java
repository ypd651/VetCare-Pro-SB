package com.veterinaria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpedienteMascotaResponse {
    private MascotaResponse mascota;
    private PropietarioResponse propietario;
    private List<ConsultaMedicaResponse> consultas;
    private List<VacunaResponse> vacunas;
    private List<CitaResponse> citas;
}
