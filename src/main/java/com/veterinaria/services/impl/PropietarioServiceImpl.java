package com.veterinaria.services.impl;

import com.veterinaria.dto.PropietarioRequest;
import com.veterinaria.dto.PropietarioResponse;
import com.veterinaria.entities.Propietario;
import com.veterinaria.exceptions.DuplicateResourceException;
import com.veterinaria.exceptions.ResourceNotFoundException;
import com.veterinaria.repository.PropietarioRepository;
import com.veterinaria.services.PropietarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropietarioServiceImpl implements PropietarioService {

    private final PropietarioRepository propietarioRepository;
    private final VeterinariaMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<PropietarioResponse> obtenerTodos(String query) {
        String normalized = mapper.normalizeNullable(query);
        List<Propietario> list = normalized == null
                ? propietarioRepository.findAllWithMascotas()
                : propietarioRepository.searchWithMascotas(normalized);
        return list.stream().map(mapper::toPropietarioResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PropietarioResponse obtenerPorId(Long id) {
        Propietario propietario = propietarioRepository.findByIdWithMascotas(id)
                .orElseThrow(() -> new ResourceNotFoundException("Propietario no encontrado con ID: " + id));
        return mapper.toPropietarioResponse(propietario);
    }

    @Override
    @Transactional
    public PropietarioResponse crearPropietario(PropietarioRequest request) {
        String email = mapper.normalizeNullable(request.getEmail());
        String documento = mapper.normalizeNullable(request.getDocumento());

        validarDuplicados(email, documento, null);

        Propietario propietario = Propietario.builder()
                .nombre(mapper.normalizeRequired(request.getNombre()))
                .documento(documento)
                .telefono(mapper.normalizeRequired(request.getTelefono()))
                .email(email)
                .direccion(mapper.normalizeNullable(request.getDireccion()))
                .notas(mapper.normalizeNullable(request.getNotas()))
                .build();

        return mapper.toPropietarioResponse(propietarioRepository.save(propietario));
    }

    @Override
    @Transactional
    public PropietarioResponse actualizarPropietario(Long id, PropietarioRequest request) {
        Propietario propietario = propietarioRepository.findByIdWithMascotas(id)
                .orElseThrow(() -> new ResourceNotFoundException("Propietario no encontrado con ID: " + id));

        String email = mapper.normalizeNullable(request.getEmail());
        String documento = mapper.normalizeNullable(request.getDocumento());

        validarDuplicados(email, documento, id);

        propietario.setNombre(mapper.normalizeRequired(request.getNombre()));
        propietario.setDocumento(documento);
        propietario.setTelefono(mapper.normalizeRequired(request.getTelefono()));
        propietario.setEmail(email);
        propietario.setDireccion(mapper.normalizeNullable(request.getDireccion()));
        propietario.setNotas(mapper.normalizeNullable(request.getNotas()));

        return mapper.toPropietarioResponse(propietarioRepository.save(propietario));
    }

    @Override
    @Transactional
    public void eliminarPropietario(Long id) {
        Propietario propietario = propietarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Propietario no encontrado con ID: " + id));
        propietarioRepository.delete(propietario);
    }

    private void validarDuplicados(String email, String documento, Long excludeId) {
        if (email != null) {
            boolean emailExists = excludeId == null
                    ? propietarioRepository.existsByEmailIgnoreCase(email)
                    : propietarioRepository.existsByEmailIgnoreCaseAndIdNot(email, excludeId);
            if (emailExists) {
                throw new DuplicateResourceException("Ya existe un propietario registrado con el correo: " + email);
            }
        }
        if (documento != null) {
            boolean docExists = excludeId == null
                    ? propietarioRepository.existsByDocumentoIgnoreCase(documento)
                    : propietarioRepository.existsByDocumentoIgnoreCaseAndIdNot(documento, excludeId);
            if (docExists) {
                throw new DuplicateResourceException("Ya existe un propietario registrado con el documento: " + documento);
            }
        }
    }
}
