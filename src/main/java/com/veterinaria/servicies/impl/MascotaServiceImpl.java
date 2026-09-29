package com.veterinaria.servicies.impl;

import com.veterinaria.dto.CreateMascotaRequest;
import com.veterinaria.dto.MascotaResponse;
import com.veterinaria.entities.Mascota;
import com.veterinaria.entities.Propietario;
import com.veterinaria.repository.MascotaRepository;
import com.veterinaria.repository.PropietarioRepository;
import com.veterinaria.servicies.MascotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MascotaServiceImpl implements MascotaService {

    private final MascotaRepository mascotaRepository;
    private final PropietarioRepository propietarioRepository;

    @Override
    public List<MascotaResponse> obtenerTodas() {
        return mascotaRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public MascotaResponse obtenerPorId(Long id) {
        Mascota mascota = mascotaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mascota no encontrada"));
        return mapToResponse(mascota);
    }

    @Override
    public MascotaResponse crearMascota(CreateMascotaRequest request) {
        Propietario propietario = propietarioRepository.findById(request.getPropietarioId())
                .orElseThrow(() -> new RuntimeException("Propietario no encontrado con ID: " + request.getPropietarioId()));

        Mascota mascota = Mascota.builder()
                .nombre(request.getNombre())
                .especie(request.getEspecie())
                .raza(request.getRaza())
                .edadAnios(request.getEdadAnios())
                .propietario(propietario)
                .build();

        return mapToResponse(mascotaRepository.save(mascota));
    }

    @Override
    public List<MascotaResponse> obtenerPorPropietario(Long propietarioId) {
        return mascotaRepository.findByPropietarioId(propietarioId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void eliminarMascota(Long id) {
        if (!mascotaRepository.existsById(id)) {
            throw new RuntimeException("Mascota no encontrada");
        }
        mascotaRepository.deleteById(id);
    }

    private MascotaResponse mapToResponse(Mascota mascota) {
        return MascotaResponse.builder()
                .id(mascota.getId())
                .nombre(mascota.getNombre())
                .especie(mascota.getEspecie())
                .raza(mascota.getRaza())
                .edadAnios(mascota.getEdadAnios())
                .nombrePropietario(mascota.getPropietario() != null ? mascota.getPropietario().getNombre() : "Sin Propietario")
                .build();
    }
}