package com.veterinaria.services;

import com.veterinaria.dto.ExpedienteMascotaResponse;
import com.veterinaria.dto.MascotaRequest;
import com.veterinaria.dto.MascotaResponse;

import java.util.List;

public interface MascotaService {
    List<MascotaResponse> obtenerTodas(String query, String especie, Long propietarioId);
    MascotaResponse obtenerPorId(Long id);
    ExpedienteMascotaResponse obtenerExpediente(Long id);
    List<MascotaResponse> obtenerPorPropietario(Long propietarioId);
    MascotaResponse crearMascota(MascotaRequest request);
    MascotaResponse actualizarMascota(Long id, MascotaRequest request);
    void eliminarMascota(Long id);
}
