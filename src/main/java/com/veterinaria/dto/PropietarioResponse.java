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
public class PropietarioResponse {
    private Long id;
    private String nombre;
    private String documento;
    private String telefono;
    private String email;
    private String direccion;
    private String notas;
    private LocalDateTime fechaRegistro;
    private int cantidadMascotas;
}
