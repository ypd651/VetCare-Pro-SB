package com.veterinaria.services.impl;

import com.veterinaria.dto.CitaRequest;
import com.veterinaria.dto.CitaResponse;
import com.veterinaria.entities.*;
import com.veterinaria.exceptions.BusinessRuleException;
import com.veterinaria.exceptions.ResourceNotFoundException;
import com.veterinaria.repository.CitaRepository;
import com.veterinaria.repository.ConsultaMedicaRepository;
import com.veterinaria.repository.MascotaRepository;
import com.veterinaria.services.CitaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CitaServiceImpl implements CitaService {

    private static final int VENTANA_CONFLICTO_MINUTOS = 29;
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CitaRepository citaRepository;
    private final MascotaRepository mascotaRepository;
    private final ConsultaMedicaRepository consultaMedicaRepository;
    private final VeterinariaMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<CitaResponse> obtenerTodas(EstadoCita estado, PrioridadCita prioridad, String fecha, Long mascotaId, String query) {
        List<Cita> citas = mascotaId != null
                ? citaRepository.findByMascotaIdWithDetails(mascotaId)
                : citaRepository.findAllWithDetails();

        String qLower = mapper.normalizeNullable(query);
        String qSearch = qLower != null ? qLower.toLowerCase() : null;
        String fechaNorm = mapper.normalizeNullable(fecha);

        return citas.stream()
                .filter(c -> estado == null || c.getEstado() == estado)
                .filter(c -> prioridad == null || c.getPrioridad() == prioridad)
                .filter(c -> matchesFechaFilter(c, fechaNorm))
                .filter(c -> qSearch == null || matchesQuery(c, qSearch))
                .map(mapper::toCitaResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CitaResponse obtenerPorId(Long id) {
        Cita cita = citaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));
        return mapper.toCitaResponse(cita);
    }

    @Override
    @Transactional
    public CitaResponse crearCita(CitaRequest request) {
        Mascota mascota = mascotaRepository.findByIdWithPropietario(request.getMascotaId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con ID: " + request.getMascotaId()));

        String veterinario = mapper.normalizeRequired(request.getVeterinario());
        validarConflictosAgenda(veterinario, mascota.getId(), request.getFechaHora(), null);

        PrioridadCita prioridad = request.getPrioridad() != null
                ? request.getPrioridad()
                : (request.getTipo() == TipoCita.URGENCIA ? PrioridadCita.URGENTE : PrioridadCita.NORMAL);

        Cita cita = Cita.builder()
                .mascota(mascota)
                .fechaHora(request.getFechaHora())
                .tipo(request.getTipo())
                .prioridad(prioridad)
                .estado(EstadoCita.PROGRAMADA)
                .veterinario(veterinario)
                .motivo(mapper.normalizeRequired(request.getMotivo()))
                .notas(mapper.normalizeNullable(request.getNotas()))
                .build();

        return mapper.toCitaResponse(citaRepository.save(cita));
    }

    @Override
    @Transactional
    public CitaResponse actualizarCita(Long id, CitaRequest request) {
        Cita cita = citaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));

        Mascota mascota = mascotaRepository.findByIdWithPropietario(request.getMascotaId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con ID: " + request.getMascotaId()));

        String veterinario = mapper.normalizeRequired(request.getVeterinario());
        if (cita.getEstado() != EstadoCita.CANCELADA) {
            validarConflictosAgenda(veterinario, mascota.getId(), request.getFechaHora(), id);
        }

        cita.setMascota(mascota);
        cita.setFechaHora(request.getFechaHora());
        cita.setTipo(request.getTipo());
        if (request.getPrioridad() != null) {
            cita.setPrioridad(request.getPrioridad());
        }
        cita.setVeterinario(veterinario);
        cita.setMotivo(mapper.normalizeRequired(request.getMotivo()));
        cita.setNotas(mapper.normalizeNullable(request.getNotas()));

        return mapper.toCitaResponse(citaRepository.save(cita));
    }

    @Override
    @Transactional
    public CitaResponse cambiarEstado(Long id, EstadoCita nuevoEstado) {
        Cita cita = citaRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));

        EstadoCita actual = cita.getEstado();
        if (actual == EstadoCita.COMPLETADA && nuevoEstado == EstadoCita.CANCELADA) {
            throw new BusinessRuleException("Una cita ya completada no puede marcarse como cancelada.");
        }
        if (actual == EstadoCita.CANCELADA && nuevoEstado == EstadoCita.COMPLETADA) {
            throw new BusinessRuleException("Una cita cancelada debe reprogramarse antes de completarse.");
        }
        if (actual == EstadoCita.CANCELADA && (nuevoEstado == EstadoCita.PROGRAMADA || nuevoEstado == EstadoCita.EN_ATENCION)) {
            validarConflictosAgenda(cita.getVeterinario(), cita.getMascota().getId(), cita.getFechaHora(), id);
        }

        cita.setEstado(nuevoEstado);
        return mapper.toCitaResponse(citaRepository.save(cita));
    }

    @Override
    @Transactional
    public void eliminarCita(Long id) {
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con ID: " + id));
        consultaMedicaRepository.clearCitaReferences(id);
        citaRepository.delete(cita);
    }

    private void validarConflictosAgenda(String veterinario, Long mascotaId, LocalDateTime fechaHora, Long excludeCitaId) {
        LocalDateTime start = fechaHora.minusMinutes(VENTANA_CONFLICTO_MINUTOS);
        LocalDateTime end = fechaHora.plusMinutes(VENTANA_CONFLICTO_MINUTOS);

        List<Cita> vetConflicts = citaRepository.findActiveConflictsForVet(veterinario, start, end).stream()
                .filter(c -> excludeCitaId == null || !c.getId().equals(excludeCitaId))
                .toList();

        if (!vetConflicts.isEmpty()) {
            Cita c = vetConflicts.get(0);
            throw new BusinessRuleException(
                    "Conflicto de agenda: " + veterinario + " ya tiene una cita con "
                            + c.getMascota().getNombre() + " el " + c.getFechaHora().format(HORA_FMT) + ".",
                    HttpStatus.CONFLICT
            );
        }

        List<Cita> petConflicts = citaRepository.findActiveConflictsForMascota(mascotaId, start, end).stream()
                .filter(c -> excludeCitaId == null || !c.getId().equals(excludeCitaId))
                .toList();

        if (!petConflicts.isEmpty()) {
            Cita c = petConflicts.get(0);
            throw new BusinessRuleException(
                    "Conflicto de agenda: la mascota ya tiene otra cita programada el "
                            + c.getFechaHora().format(HORA_FMT) + ".",
                    HttpStatus.CONFLICT
            );
        }
    }

    private boolean matchesFechaFilter(Cita cita, String fechaFilter) {
        if (fechaFilter == null) {
            return true;
        }
        LocalDate citaDate = cita.getFechaHora().toLocalDate();
        LocalDate today = LocalDate.now();
        if ("hoy".equalsIgnoreCase(fechaFilter)) {
            return citaDate.equals(today);
        }
        if ("proximas".equalsIgnoreCase(fechaFilter)) {
            return !citaDate.isBefore(today) && cita.getEstado() != EstadoCita.CANCELADA && cita.getEstado() != EstadoCita.COMPLETADA;
        }
        try {
            return citaDate.equals(LocalDate.parse(fechaFilter));
        } catch (DateTimeParseException ex) {
            throw new BusinessRuleException("Filtro de fecha inválido. Usa 'hoy', 'proximas' o formato YYYY-MM-DD.");
        }
    }

    private boolean matchesQuery(Cita c, String qLower) {
        return mapper.containsIgnoreCase(c.getVeterinario(), qLower)
                || mapper.containsIgnoreCase(c.getMotivo(), qLower)
                || (c.getMascota() != null && mapper.containsIgnoreCase(c.getMascota().getNombre(), qLower))
                || (c.getMascota() != null && c.getMascota().getPropietario() != null
                && mapper.containsIgnoreCase(c.getMascota().getPropietario().getNombre(), qLower));
    }
}
