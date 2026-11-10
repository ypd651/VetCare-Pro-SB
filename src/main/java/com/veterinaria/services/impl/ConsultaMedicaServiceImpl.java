package com.veterinaria.services.impl;

import com.veterinaria.dto.ConsultaMedicaRequest;
import com.veterinaria.dto.ConsultaMedicaResponse;
import com.veterinaria.entities.Cita;
import com.veterinaria.entities.ConsultaMedica;
import com.veterinaria.entities.EstadoCita;
import com.veterinaria.entities.Mascota;
import com.veterinaria.exceptions.BusinessRuleException;
import com.veterinaria.exceptions.ResourceNotFoundException;
import com.veterinaria.repository.CitaRepository;
import com.veterinaria.repository.ConsultaMedicaRepository;
import com.veterinaria.repository.MascotaRepository;
import com.veterinaria.services.ConsultaMedicaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultaMedicaServiceImpl implements ConsultaMedicaService {

    private final ConsultaMedicaRepository consultaRepository;
    private final MascotaRepository mascotaRepository;
    private final CitaRepository citaRepository;
    private final VeterinariaMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ConsultaMedicaResponse> obtenerTodas(Long mascotaId, String query) {
        List<ConsultaMedica> list = mascotaId != null
                ? consultaRepository.findByMascotaIdWithDetails(mascotaId)
                : consultaRepository.findAllWithDetails();

        String qNorm = mapper.normalizeNullable(query);
        String qLower = qNorm != null ? qNorm.toLowerCase() : null;

        return list.stream()
                .filter(cm -> qLower == null || matchesQuery(cm, qLower))
                .map(mapper::toConsultaResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultaMedicaResponse obtenerPorId(Long id) {
        ConsultaMedica consulta = consultaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta médica no encontrada con ID: " + id));
        return mapper.toConsultaResponse(consulta);
    }

    @Override
    @Transactional
    public ConsultaMedicaResponse crearConsulta(ConsultaMedicaRequest request) {
        Mascota mascota = mascotaRepository.findByIdWithPropietario(request.getMascotaId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con ID: " + request.getMascotaId()));

        LocalDateTime fechaConsulta = request.getFechaConsulta() != null
                ? request.getFechaConsulta()
                : LocalDateTime.now();

        validarProximaRevision(request, fechaConsulta);

        Cita cita = resolverYCompletarCita(request.getCitaId(), mascota.getId());

        ConsultaMedica consulta = ConsultaMedica.builder()
                .mascota(mascota)
                .cita(cita)
                .fechaConsulta(fechaConsulta)
                .veterinario(mapper.normalizeRequired(request.getVeterinario()))
                .motivoConsulta(mapper.normalizeRequired(request.getMotivoConsulta()))
                .sintomas(mapper.normalizeNullable(request.getSintomas()))
                .diagnostico(mapper.normalizeRequired(request.getDiagnostico()))
                .tratamiento(mapper.normalizeRequired(request.getTratamiento()))
                .pesoKg(request.getPesoKg())
                .temperaturaC(request.getTemperaturaC())
                .frecuenciaCardiaca(request.getFrecuenciaCardiaca())
                .proximaRevision(request.getProximaRevision())
                .observaciones(mapper.normalizeNullable(request.getObservaciones()))
                .build();

        if (request.getPesoKg() != null && request.getPesoKg() > 0) {
            mascota.setPesoKg(request.getPesoKg());
            mascotaRepository.save(mascota);
        }

        return mapper.toConsultaResponse(consultaRepository.save(consulta));
    }

    @Override
    @Transactional
    public ConsultaMedicaResponse actualizarConsulta(Long id, ConsultaMedicaRequest request) {
        ConsultaMedica consulta = consultaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta médica no encontrada con ID: " + id));

        Mascota mascota = mascotaRepository.findByIdWithPropietario(request.getMascotaId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con ID: " + request.getMascotaId()));

        LocalDateTime fechaConsulta = request.getFechaConsulta() != null
                ? request.getFechaConsulta()
                : consulta.getFechaConsulta();

        validarProximaRevision(request, fechaConsulta);
        Cita cita = resolverYCompletarCita(request.getCitaId(), mascota.getId());

        consulta.setMascota(mascota);
        consulta.setCita(cita);
        consulta.setFechaConsulta(fechaConsulta);
        consulta.setVeterinario(mapper.normalizeRequired(request.getVeterinario()));
        consulta.setMotivoConsulta(mapper.normalizeRequired(request.getMotivoConsulta()));
        consulta.setSintomas(mapper.normalizeNullable(request.getSintomas()));
        consulta.setDiagnostico(mapper.normalizeRequired(request.getDiagnostico()));
        consulta.setTratamiento(mapper.normalizeRequired(request.getTratamiento()));
        consulta.setPesoKg(request.getPesoKg());
        consulta.setTemperaturaC(request.getTemperaturaC());
        consulta.setFrecuenciaCardiaca(request.getFrecuenciaCardiaca());
        consulta.setProximaRevision(request.getProximaRevision());
        consulta.setObservaciones(mapper.normalizeNullable(request.getObservaciones()));

        if (request.getPesoKg() != null && request.getPesoKg() > 0) {
            mascota.setPesoKg(request.getPesoKg());
            mascotaRepository.save(mascota);
        }

        return mapper.toConsultaResponse(consultaRepository.save(consulta));
    }

    @Override
    @Transactional
    public void eliminarConsulta(Long id) {
        ConsultaMedica consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta médica no encontrada con ID: " + id));
        consultaRepository.delete(consulta);
    }

    private Cita resolverYCompletarCita(Long citaId, Long mascotaId) {
        if (citaId == null) {
            return null;
        }
        Cita cita = citaRepository.findByIdWithDetails(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + citaId));
        if (!cita.getMascota().getId().equals(mascotaId)) {
            throw new BusinessRuleException("La cita seleccionada pertenece a otra mascota.");
        }
        if (cita.getEstado() != EstadoCita.CANCELADA && cita.getEstado() != EstadoCita.COMPLETADA) {
            cita.setEstado(EstadoCita.COMPLETADA);
            citaRepository.save(cita);
        }
        return cita;
    }

    private void validarProximaRevision(ConsultaMedicaRequest request, LocalDateTime fechaConsulta) {
        if (request.getProximaRevision() != null && request.getProximaRevision().isBefore(fechaConsulta.toLocalDate())) {
            throw new BusinessRuleException("La fecha de próxima revisión no puede ser anterior a la fecha de consulta.");
        }
    }

    private boolean matchesQuery(ConsultaMedica cm, String qLower) {
        return mapper.containsIgnoreCase(cm.getVeterinario(), qLower)
                || mapper.containsIgnoreCase(cm.getMotivoConsulta(), qLower)
                || mapper.containsIgnoreCase(cm.getDiagnostico(), qLower)
                || mapper.containsIgnoreCase(cm.getTratamiento(), qLower)
                || (cm.getMascota() != null && mapper.containsIgnoreCase(cm.getMascota().getNombre(), qLower));
    }
}
