package com.veterinaria.servicies;

import com.veterinaria.dto.CreatePropietarioRequest;
import com.veterinaria.dto.PropietarioResponse;
import java.util.List;

public interface PropietarioService {
    List<PropietarioResponse> obtenerTodos();
    PropietarioResponse obtenerPorId(Long id);
    PropietarioResponse crearPropietario(CreatePropietarioRequest request);
    void eliminarPropietario(Long id);
}