package com.veterinaria.dto;

import lombok.Data;

@Data
public class CreatePropietarioRequest {
    private String nombre;
    private String telefono;
    private String email;
}