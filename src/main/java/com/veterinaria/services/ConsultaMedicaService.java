package com.veterinaria.services;

import com.veterinaria.dto.ConsultaMedicaRequest;
import com.veterinaria.dto.ConsultaMedicaResponse;

import java.util.List;

public interface ConsultaMedicaService {
    List<ConsultaMedicaResponse> obtenerTodas(Long mascotaId, String query);
    ConsultaMedicaResponse obtenerPorId(Long id);
    ConsultaMedicaResponse crearConsulta(ConsultaMedicaRequest request);
    ConsultaMedicaResponse actualizarConsulta(Long id, ConsultaMedicaRequest request);
    void eliminarConsulta(Long id);
}
