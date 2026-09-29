package com.veterinaria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PropietarioResponse {
    private Long id;
    private String nombre;
    private String telefono;
    private String email;
    private int cantidadMascotas;
}