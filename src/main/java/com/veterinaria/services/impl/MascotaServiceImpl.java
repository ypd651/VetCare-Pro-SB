package com.veterinaria.services.impl;

import com.veterinaria.dto.*;
import com.veterinaria.entities.Mascota;
import com.veterinaria.entities.Propietario;
import com.veterinaria.exceptions.DuplicateResourceException;
import com.veterinaria.exceptions.ResourceNotFoundException;
import com.veterinaria.repository.*;
import com.veterinaria.services.MascotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MascotaServiceImpl implements MascotaService {

    private final MascotaRepository mascotaRepository;
    private final PropietarioRepository propietarioRepository;
    private final ConsultaMedicaRepository consultaRepository;
    private final VacunaRepository vacunaRepository;
    private final CitaRepository citaRepository;
    private final VeterinariaMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<MascotaResponse> obtenerTodas(String query, String especie, Long propietarioId) {
        String normalizedQuery = mapper.normalizeNullable(query);
        String qLower = normalizedQuery != null ? normalizedQuery.toLowerCase() : null;
        String especieNorm = mapper.normalizeNullable(especie);

        List<Mascota> mascotas = propietarioId != null
                ? mascotaRepository.findByPropietarioIdWithPropietario(propietarioId)
                : mascotaRepository.findAllWithPropietario();

        Map<Long, Long> consultasPorMascota = buildConsultasCountMap();
        Map<Long, Long> alertasPorMascota = buildAlertasVacunasCountMap();

        return mascotas.stream()
                .filter(m -> especieNorm == null || m.getEspecie().equalsIgnoreCase(especieNorm))
                .filter(m -> qLower == null || matchesQuery(m, qLower))
                .map(m -> mapper.toMascotaResponse(
                        m,
                        consultasPorMascota.getOrDefault(m.getId(), 0L),
                        alertasPorMascota.getOrDefault(m.getId(), 0L)
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MascotaResponse obtenerPorId(Long id) {
        Mascota mascota = mascotaRepository.findByIdWithPropietario(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con ID: " + id));
        return toDetailedMascotaResponse(mascota);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpedienteMascotaResponse obtenerExpediente(Long id) {
        Mascota mascota = mascotaRepository.findByIdWithPropietario(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con ID: " + id));

        Propietario propietario = propietarioRepository.findByIdWithMascotas(mascota.getPropietario().getId())
                .orElse(mascota.getPropietario());

        LocalDate today = LocalDate.now();
        List<ConsultaMedicaResponse> consultas = consultaRepository.findByMascotaIdWithDetails(id).stream()
                .map(mapper::toConsultaResponse)
                .toList();
        List<VacunaResponse> vacunas = vacunaRepository.findByMascotaIdWithDetails(id).stream()
                .map(v -> mapper.toVacunaResponse(v, today))
                .toList();
        List<CitaResponse> citas = citaRepository.findByMascotaIdWithDetails(id).stream()
                .map(mapper::toCitaResponse)
                .toList();

        long alertasCount = vacunas.stream()
                .filter(v -> v.getEstadoAlerta().name().equals("VENCIDA") || v.getEstadoAlerta().name().equals("PROXIMA"))
                .count();

        return ExpedienteMascotaResponse.builder()
                .mascota(mapper.toMascotaResponse(mascota, consultas.size(), alertasCount))
                .propietario(mapper.toPropietarioResponse(propietario))
                .consultas(consultas)
                .vacunas(vacunas)
                .citas(citas)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MascotaResponse> obtenerPorPropietario(Long propietarioId) {
        if (!propietarioRepository.existsById(propietarioId)) {
            throw new ResourceNotFoundException("Propietario no encontrado con ID: " + propietarioId);
        }
        return obtenerTodas(null, null, propietarioId);
    }

    @Override
    @Transactional
    public MascotaResponse crearMascota(MascotaRequest request) {
        Propietario propietario = propietarioRepository.findById(request.getPropietarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Propietario no encontrado con ID: " + request.getPropietarioId()));

        String microchip = mapper.normalizeNullable(request.getMicrochip());
        validarMicrochipUnico(microchip, null);

        Mascota mascota = Mascota.builder()
                .nombre(mapper.normalizeRequired(request.getNombre()))
                .especie(mapper.normalizeRequired(request.getEspecie()))
                .raza(mapper.normalizeNullable(request.getRaza()))
                .sexo(mapper.normalizeNullable(request.getSexo()))
                .edadAnios(request.getEdadAnios())
                .pesoKg(request.getPesoKg())
                .color(mapper.normalizeNullable(request.getColor()))
                .microchip(microchip)
                .esterilizado(Boolean.TRUE.equals(request.getEsterilizado()))
                .alergias(mapper.normalizeNullable(request.getAlergias()))
                .notasMedicas(mapper.normalizeNullable(request.getNotasMedicas()))
                .propietario(propietario)
                .build();

        Mascota saved = mascotaRepository.save(mascota);
        return mapper.toMascotaResponse(saved, 0L, 0L);
    }

    @Override
    @Transactional
    public MascotaResponse actualizarMascota(Long id, MascotaRequest request) {
        Mascota mascota = mascotaRepository.findByIdWithPropietario(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con ID: " + id));

        Propietario propietario = propietarioRepository.findById(request.getPropietarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Propietario no encontrado con ID: " + request.getPropietarioId()));

        String microchip = mapper.normalizeNullable(request.getMicrochip());
        validarMicrochipUnico(microchip, id);

        mascota.setNombre(mapper.normalizeRequired(request.getNombre()));
        mascota.setEspecie(mapper.normalizeRequired(request.getEspecie()));
        mascota.setRaza(mapper.normalizeNullable(request.getRaza()));
        mascota.setSexo(mapper.normalizeNullable(request.getSexo()));
        mascota.setEdadAnios(request.getEdadAnios());
        mascota.setPesoKg(request.getPesoKg());
        mascota.setColor(mapper.normalizeNullable(request.getColor()));
        mascota.setMicrochip(microchip);
        mascota.setEsterilizado(Boolean.TRUE.equals(request.getEsterilizado()));
        mascota.setAlergias(mapper.normalizeNullable(request.getAlergias()));
        mascota.setNotasMedicas(mapper.normalizeNullable(request.getNotasMedicas()));
        mascota.setPropietario(propietario);

        Mascota updated = mascotaRepository.save(mascota);
        return toDetailedMascotaResponse(updated);
    }

    @Override
    @Transactional
    public void eliminarMascota(Long id) {
        Mascota mascota = mascotaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con ID: " + id));
        mascotaRepository.delete(mascota);
    }

    private MascotaResponse toDetailedMascotaResponse(Mascota mascota) {
        long consultas = consultaRepository.findByMascotaIdWithDetails(mascota.getId()).size();
        LocalDate today = LocalDate.now();
        long alertas = vacunaRepository.findByMascotaIdWithDetails(mascota.getId()).stream()
                .map(v -> mapper.calcularEstadoVacuna(v.getProximaDosis(), today))
                .filter(e -> e.name().equals("VENCIDA") || e.name().equals("PROXIMA"))
                .count();
        return mapper.toMascotaResponse(mascota, consultas, alertas);
    }

    private void validarMicrochipUnico(String microchip, Long excludeId) {
        if (microchip == null) {
            return;
        }
        boolean exists = excludeId == null
                ? mascotaRepository.existsByMicrochipIgnoreCase(microchip)
                : mascotaRepository.existsByMicrochipIgnoreCaseAndIdNot(microchip, excludeId);
        if (exists) {
            throw new DuplicateResourceException("Ya existe una mascota registrada con el microchip: " + microchip);
        }
    }

    private boolean matchesQuery(Mascota m, String qLower) {
        return mapper.containsIgnoreCase(m.getNombre(), qLower)
                || mapper.containsIgnoreCase(m.getEspecie(), qLower)
                || mapper.containsIgnoreCase(m.getRaza(), qLower)
                || mapper.containsIgnoreCase(m.getMicrochip(), qLower)
                || (m.getPropietario() != null && mapper.containsIgnoreCase(m.getPropietario().getNombre(), qLower));
    }

    private Map<Long, Long> buildConsultasCountMap() {
        return consultaRepository.countGroupedByMascota().stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private Map<Long, Long> buildAlertasVacunasCountMap() {
        LocalDate limit = LocalDate.now().plusDays(VeterinariaMapper.DIAS_ALERTA_VACUNA);
        return vacunaRepository.findAlertasUntilWithDetails(limit).stream()
                .collect(Collectors.groupingBy(v -> v.getMascota().getId(), Collectors.counting()));
    }
}
