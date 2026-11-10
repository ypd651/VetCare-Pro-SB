package com.veterinaria.services.impl;

import com.veterinaria.dto.VacunaRequest;
import com.veterinaria.dto.VacunaResponse;
import com.veterinaria.entities.EstadoVacuna;
import com.veterinaria.entities.Mascota;
import com.veterinaria.entities.TipoInmunizacion;
import com.veterinaria.entities.Vacuna;
import com.veterinaria.exceptions.BusinessRuleException;
import com.veterinaria.exceptions.ResourceNotFoundException;
import com.veterinaria.repository.MascotaRepository;
import com.veterinaria.repository.VacunaRepository;
import com.veterinaria.services.VacunaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VacunaServiceImpl implements VacunaService {

    private final VacunaRepository vacunaRepository;
    private final MascotaRepository mascotaRepository;
    private final VeterinariaMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<VacunaResponse> obtenerTodas(Long mascotaId, String estadoFiltro, TipoInmunizacion tipo, String query) {
        List<Vacuna> list = mascotaId != null
                ? vacunaRepository.findByMascotaIdWithDetails(mascotaId)
                : vacunaRepository.findAllWithDetails();

        LocalDate today = LocalDate.now();
        String estadoNorm = mapper.normalizeNullable(estadoFiltro);
        String qNorm = mapper.normalizeNullable(query);
        String qLower = qNorm != null ? qNorm.toLowerCase() : null;

        return list.stream()
                .filter(v -> tipo == null || v.getTipo() == tipo)
                .map(v -> mapper.toVacunaResponse(v, today))
                .filter(vr -> matchesEstadoFilter(vr.getEstadoAlerta(), estadoNorm))
                .filter(vr -> qLower == null || matchesQuery(vr, qLower))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VacunaResponse obtenerPorId(Long id) {
        Vacuna vacuna = vacunaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de vacuna no encontrado con ID: " + id));
        return mapper.toVacunaResponse(vacuna, LocalDate.now());
    }

    @Override
    @Transactional
    public VacunaResponse registrarVacuna(VacunaRequest request) {
        Mascota mascota = mascotaRepository.findByIdWithPropietario(request.getMascotaId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con ID: " + request.getMascotaId()));

        validarFechas(request);

        Vacuna vacuna = Vacuna.builder()
                .mascota(mascota)
                .nombre(mapper.normalizeRequired(request.getNombre()))
                .tipo(request.getTipo() != null ? request.getTipo() : TipoInmunizacion.VACUNA)
                .fechaAplicacion(request.getFechaAplicacion())
                .proximaDosis(request.getProximaDosis())
                .lote(mapper.normalizeNullable(request.getLote()))
                .veterinario(mapper.normalizeNullable(request.getVeterinario()))
                .notas(mapper.normalizeNullable(request.getNotas()))
                .build();

        return mapper.toVacunaResponse(vacunaRepository.save(vacuna), LocalDate.now());
    }

    @Override
    @Transactional
    public VacunaResponse actualizarVacuna(Long id, VacunaRequest request) {
        Vacuna vacuna = vacunaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de vacuna no encontrado con ID: " + id));

        Mascota mascota = mascotaRepository.findByIdWithPropietario(request.getMascotaId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con ID: " + request.getMascotaId()));

        validarFechas(request);

        vacuna.setMascota(mascota);
        vacuna.setNombre(mapper.normalizeRequired(request.getNombre()));
        vacuna.setTipo(request.getTipo() != null ? request.getTipo() : TipoInmunizacion.VACUNA);
        vacuna.setFechaAplicacion(request.getFechaAplicacion());
        vacuna.setProximaDosis(request.getProximaDosis());
        vacuna.setLote(mapper.normalizeNullable(request.getLote()));
        vacuna.setVeterinario(mapper.normalizeNullable(request.getVeterinario()));
        vacuna.setNotas(mapper.normalizeNullable(request.getNotas()));

        return mapper.toVacunaResponse(vacunaRepository.save(vacuna), LocalDate.now());
    }

    @Override
    @Transactional
    public void eliminarVacuna(Long id) {
        Vacuna vacuna = vacunaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de vacuna no encontrado con ID: " + id));
        vacunaRepository.delete(vacuna);
    }

    private void validarFechas(VacunaRequest request) {
        if (request.getProximaDosis() != null && request.getProximaDosis().isBefore(request.getFechaAplicacion())) {
            throw new BusinessRuleException("La fecha de próxima dosis no puede ser anterior a la fecha de aplicación.");
        }
    }

    private boolean matchesEstadoFilter(EstadoVacuna estado, String filtro) {
        if (filtro == null) {
            return true;
        }
        if ("ALERTAS".equalsIgnoreCase(filtro)) {
            return estado == EstadoVacuna.VENCIDA || estado == EstadoVacuna.PROXIMA;
        }
        return estado.name().equalsIgnoreCase(filtro);
    }

    private boolean matchesQuery(VacunaResponse vr, String qLower) {
        return mapper.containsIgnoreCase(vr.getNombre(), qLower)
                || mapper.containsIgnoreCase(vr.getNombreMascota(), qLower)
                || mapper.containsIgnoreCase(vr.getNombrePropietario(), qLower)
                || mapper.containsIgnoreCase(vr.getVeterinario(), qLower);
    }
}
