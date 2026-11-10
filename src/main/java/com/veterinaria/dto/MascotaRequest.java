package com.veterinaria.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MascotaRequest {

    @NotBlank(message = "El nombre de la mascota es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    @NotBlank(message = "La especie es obligatoria")
    @Size(max = 50, message = "La especie no puede exceder 50 caracteres")
    private String especie;

    @Size(max = 80, message = "La raza no puede exceder 80 caracteres")
    private String raza;

    @Size(max = 20, message = "El sexo no puede exceder 20 caracteres")
    private String sexo;

    @Min(value = 0, message = "La edad no puede ser negativa")
    @Max(value = 60, message = "La edad no puede superar 60 años")
    private Integer edadAnios;

    @DecimalMin(value = "0.0", message = "El peso no puede ser negativo")
    @DecimalMax(value = "1500.0", message = "El peso ingresado supera el límite permitido")
    private Double pesoKg;

    @Size(max = 50, message = "El color no puede exceder 50 caracteres")
    private String color;

    @Size(max = 60, message = "El código de microchip no puede exceder 60 caracteres")
    private String microchip;

    private Boolean esterilizado;

    @Size(max = 300, message = "Las alergias no pueden exceder 300 caracteres")
    private String alergias;

    @Size(max = 600, message = "Las notas médicas no pueden exceder 600 caracteres")
    private String notasMedicas;

    @NotNull(message = "Debe seleccionar un propietario")
    private Long propietarioId;
}
