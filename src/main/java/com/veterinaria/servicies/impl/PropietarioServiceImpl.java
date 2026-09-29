package com.veterinaria.servicies.impl;

import com.veterinaria.dto.CreatePropietarioRequest;
import com.veterinaria.dto.PropietarioResponse;
import com.veterinaria.entities.Propietario;
import com.veterinaria.repository.PropietarioRepository;
import com.veterinaria.servicies.PropietarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropietarioServiceImpl implements PropietarioService {

    private final PropietarioRepository propietarioRepository;

    @Override
    public List<PropietarioResponse> obtenerTodos() {
        return propietarioRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public PropietarioResponse obtenerPorId(Long id) {
        Propietario propietario = propietarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propietario no encontrado con ID: " + id));
        return mapToResponse(propietario);
    }

    @Override
    public PropietarioResponse crearPropietario(CreatePropietarioRequest request) {
        Propietario propietario = Propietario.builder()
                .nombre(request.getNombre())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .build();
        return mapToResponse(propietarioRepository.save(propietario));
    }

    @Override
    public void eliminarPropietario(Long id) {
        if (!propietarioRepository.existsById(id)) {
            throw new RuntimeException("Propietario no encontrado");
        }
        propietarioRepository.deleteById(id);
    }

    private PropietarioResponse mapToResponse(Propietario propietario) {
        return PropietarioResponse.builder()
                .id(propietario.getId())
                .nombre(propietario.getNombre())
                .telefono(propietario.getTelefono())
                .email(propietario.getEmail())
                .cantidadMascotas(propietario.getMascotas() != null ? propietario.getMascotas().size() : 0)
                .build();
    }
}