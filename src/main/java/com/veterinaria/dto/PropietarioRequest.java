package com.veterinaria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropietarioRequest {

    @NotBlank(message = "El nombre del propietario es obligatorio")
    @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
    private String nombre;

    @Size(max = 40, message = "El documento no puede exceder 40 caracteres")
    private String documento;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres")
    private String telefono;

    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 120, message = "El correo electrónico no puede exceder 120 caracteres")
    private String email;

    @Size(max = 200, message = "La dirección no puede exceder 200 caracteres")
    private String direccion;

    @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
    private String notas;
}
