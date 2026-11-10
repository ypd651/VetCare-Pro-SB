package com.veterinaria.services;

import com.veterinaria.dto.PropietarioRequest;
import com.veterinaria.dto.PropietarioResponse;

import java.util.List;

public interface PropietarioService {
    List<PropietarioResponse> obtenerTodos(String query);
    PropietarioResponse obtenerPorId(Long id);
    PropietarioResponse crearPropietario(PropietarioRequest request);
    PropietarioResponse actualizarPropietario(Long id, PropietarioRequest request);
    void eliminarPropietario(Long id);
}
