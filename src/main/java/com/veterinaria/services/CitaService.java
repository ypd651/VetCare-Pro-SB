package com.veterinaria.services;

import com.veterinaria.dto.CitaRequest;
import com.veterinaria.dto.CitaResponse;
import com.veterinaria.entities.EstadoCita;
import com.veterinaria.entities.PrioridadCita;

import java.util.List;

public interface CitaService {
    List<CitaResponse> obtenerTodas(EstadoCita estado, PrioridadCita prioridad, String fecha, Long mascotaId, String query);
    CitaResponse obtenerPorId(Long id);
    CitaResponse crearCita(CitaRequest request);
    CitaResponse actualizarCita(Long id, CitaRequest request);
    CitaResponse cambiarEstado(Long id, EstadoCita estado);
    void eliminarCita(Long id);
}
