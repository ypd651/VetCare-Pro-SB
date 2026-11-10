package com.veterinaria.services.impl;

import com.veterinaria.dto.*;
import com.veterinaria.entities.*;
import com.veterinaria.repository.*;
import com.veterinaria.services.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final PropietarioRepository propietarioRepository;
    private final MascotaRepository mascotaRepository;
    private final CitaRepository citaRepository;
    private final ConsultaMedicaRepository consultaRepository;
    private final VacunaRepository vacunaRepository;
    private final VeterinariaMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse obtenerResumen() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        long totalPropietarios = propietarioRepository.count();
        long totalMascotas = mascotaRepository.count();

        List<Cita> citasHoyEntidades = citaRepository.findByFechaHoraBetweenWithDetails(startOfDay, endOfDay);
        List<CitaResponse> citasHoyLista = citasHoyEntidades.stream()
                .sorted(Comparator
                        .comparing((Cita c) -> c.getPrioridad() == PrioridadCita.URGENTE ? 0 : c.getPrioridad() == PrioridadCita.ALTA ? 1 : 2)
                        .thenComparing(Cita::getFechaHora))
                .map(mapper::toCitaResponse)
                .toList();

        long citasPendientes = citaRepository.countByEstadoIn(List.of(EstadoCita.PROGRAMADA, EstadoCita.EN_ATENCION));
        long urgenciasActivas = citaRepository.findAllWithDetails().stream()
                .filter(c -> c.getPrioridad() == PrioridadCita.URGENTE
                        && (c.getEstado() == EstadoCita.PROGRAMADA || c.getEstado() == EstadoCita.EN_ATENCION))
                .count();

        LocalDate limitAlertDate = today.plusDays(VeterinariaMapper.DIAS_ALERTA_VACUNA);
        List<VacunaResponse> alertasVacunas = vacunaRepository.findAlertasUntilWithDetails(limitAlertDate).stream()
                .map(v -> mapper.toVacunaResponse(v, today))
                .toList();

        long vacunasVencidas = alertasVacunas.stream()
                .filter(v -> v.getEstadoAlerta() == EstadoVacuna.VENCIDA)
                .count();
        long vacunasProximas = alertasVacunas.stream()
                .filter(v -> v.getEstadoAlerta() == EstadoVacuna.PROXIMA)
                .count();

        List<DashboardResponse.EspecieStat> especies = mascotaRepository.countByEspecie().stream()
                .map(row -> DashboardResponse.EspecieStat.builder()
                        .especie((String) row[0])
                        .cantidad((Long) row[1])
                        .build())
                .toList();

        List<ConsultaMedicaResponse> proximasRevisiones = consultaRepository
                .findUpcomingRevisionsWithDetails(today, today.plusDays(14))
                .stream()
                .map(mapper::toConsultaResponse)
                .toList();

        List<ConsultaMedicaResponse> consultasRecientes = consultaRepository.findAllWithDetails().stream()
                .limit(5)
                .map(mapper::toConsultaResponse)
                .toList();

        long consultasMes = consultaRepository.countByFechaConsultaAfter(today.withDayOfMonth(1).atStartOfDay());

        return DashboardResponse.builder()
                .totalPropietarios(totalPropietarios)
                .totalMascotas(totalMascotas)
                .citasHoy(citasHoyLista.size())
                .citasPendientes(citasPendientes)
                .urgenciasActivas(urgenciasActivas)
                .vacunasVencidas(vacunasVencidas)
                .vacunasProximas(vacunasProximas)
                .consultasMes(consultasMes)
                .distribucionEspecies(especies)
                .citasHoyLista(citasHoyLista)
                .alertasVacunas(alertasVacunas)
                .proximasRevisiones(proximasRevisiones)
                .consultasRecientes(consultasRecientes)
                .build();
    }
}
