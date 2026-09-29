package com.veterinaria.dto;

import lombok.Data;

@Data
public class CreateMascotaRequest {
    private String nombre;
    private String especie;
    private String raza;
    private Integer edadAnios;
    private Long propietarioId;
}