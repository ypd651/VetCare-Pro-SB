package com.veterinaria.services;

import com.veterinaria.dto.VacunaRequest;
import com.veterinaria.dto.VacunaResponse;
import com.veterinaria.entities.TipoInmunizacion;

import java.util.List;

public interface VacunaService {
    List<VacunaResponse> obtenerTodas(Long mascotaId, String estado, TipoInmunizacion tipo, String query);
    VacunaResponse obtenerPorId(Long id);
    VacunaResponse registrarVacuna(VacunaRequest request);
    VacunaResponse actualizarVacuna(Long id, VacunaRequest request);
    void eliminarVacuna(Long id);
}
