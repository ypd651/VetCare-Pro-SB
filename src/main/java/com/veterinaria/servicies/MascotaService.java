package com.veterinaria.servicies;

import com.veterinaria.dto.CreateMascotaRequest;
import com.veterinaria.dto.MascotaResponse;
import java.util.List;

public interface MascotaService {
    List<MascotaResponse> obtenerTodas();
    MascotaResponse obtenerPorId(Long id);
    MascotaResponse crearMascota(CreateMascotaRequest request);
    List<MascotaResponse> obtenerPorPropietario(Long propietarioId);
    void eliminarMascota(Long id);
}