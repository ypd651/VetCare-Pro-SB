package com.veterinaria.services.impl;

import com.veterinaria.dto.*;
import com.veterinaria.entities.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class VeterinariaMapper {

    public static final int DIAS_ALERTA_VACUNA = 15;

    public String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    public boolean containsIgnoreCase(String source, String query) {
        return source != null && source.toLowerCase().contains(query);
    }

    public PropietarioResponse toPropietarioResponse(Propietario p) {
        return PropietarioResponse.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .documento(p.getDocumento())
                .telefono(p.getTelefono())
                .email(p.getEmail())
                .direccion(p.getDireccion())
                .notas(p.getNotas())
                .fechaRegistro(p.getFechaRegistro())
                .cantidadMascotas(p.getMascotas() != null ? p.getMascotas().size() : 0)
                .build();
    }

    public MascotaResponse toMascotaResponse(Mascota m, long totalConsultas, long alertasVacunas) {
        Propietario prop = m.getPropietario();
        return MascotaResponse.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .especie(m.getEspecie())
                .raza(m.getRaza())
                .sexo(m.getSexo())
                .edadAnios(m.getEdadAnios())
                .pesoKg(m.getPesoKg())
                .color(m.getColor())
                .microchip(m.getMicrochip())
                .esterilizado(Boolean.TRUE.equals(m.getEsterilizado()))
                .alergias(m.getAlergias())
                .notasMedicas(m.getNotasMedicas())
                .fechaRegistro(m.getFechaRegistro())
                .propietarioId(prop != null ? prop.getId() : null)
                .nombrePropietario(prop != null ? prop.getNombre() : "Sin Propietario")
                .telefonoPropietario(prop != null ? prop.getTelefono() : null)
                .totalConsultas(totalConsultas)
                .alertasVacunas(alertasVacunas)
                .build();
    }

    public CitaResponse toCitaResponse(Cita c) {
        Mascota m = c.getMascota();
        Propietario p = m != null ? m.getPropietario() : null;
        return CitaResponse.builder()
                .id(c.getId())
                .mascotaId(m != null ? m.getId() : null)
                .nombreMascota(m != null ? m.getNombre() : null)
                .especieMascota(m != null ? m.getEspecie() : null)
                .propietarioId(p != null ? p.getId() : null)
                .nombrePropietario(p != null ? p.getNombre() : null)
                .telefonoPropietario(p != null ? p.getTelefono() : null)
                .fechaHora(c.getFechaHora())
                .tipo(c.getTipo())
                .prioridad(c.getPrioridad())
                .estado(c.getEstado())
                .veterinario(c.getVeterinario())
                .motivo(c.getMotivo())
                .notas(c.getNotas())
                .fechaCreacion(c.getFechaCreacion())
                .build();
    }

    public ConsultaMedicaResponse toConsultaResponse(ConsultaMedica cm) {
        Mascota m = cm.getMascota();
        Propietario p = m != null ? m.getPropietario() : null;
        return ConsultaMedicaResponse.builder()
                .id(cm.getId())
                .mascotaId(m != null ? m.getId() : null)
                .nombreMascota(m != null ? m.getNombre() : null)
                .especieMascota(m != null ? m.getEspecie() : null)
                .nombrePropietario(p != null ? p.getNombre() : null)
                .telefonoPropietario(p != null ? p.getTelefono() : null)
                .citaId(cm.getCita() != null ? cm.getCita().getId() : null)
                .fechaConsulta(cm.getFechaConsulta())
                .veterinario(cm.getVeterinario())
                .motivoConsulta(cm.getMotivoConsulta())
                .sintomas(cm.getSintomas())
                .diagnostico(cm.getDiagnostico())
                .tratamiento(cm.getTratamiento())
                .pesoKg(cm.getPesoKg())
                .temperaturaC(cm.getTemperaturaC())
                .frecuenciaCardiaca(cm.getFrecuenciaCardiaca())
                .proximaRevision(cm.getProximaRevision())
                .observaciones(cm.getObservaciones())
                .build();
    }

    public VacunaResponse toVacunaResponse(Vacuna v, LocalDate today) {
        Mascota m = v.getMascota();
        Propietario p = m != null ? m.getPropietario() : null;
        EstadoVacuna estadoAlerta = calcularEstadoVacuna(v.getProximaDosis(), today);
        Long dias = v.getProximaDosis() != null ? ChronoUnit.DAYS.between(today, v.getProximaDosis()) : null;

        return VacunaResponse.builder()
                .id(v.getId())
                .mascotaId(m != null ? m.getId() : null)
                .nombreMascota(m != null ? m.getNombre() : null)
                .especieMascota(m != null ? m.getEspecie() : null)
                .propietarioId(p != null ? p.getId() : null)
                .nombrePropietario(p != null ? p.getNombre() : null)
                .telefonoPropietario(p != null ? p.getTelefono() : null)
                .emailPropietario(p != null ? p.getEmail() : null)
                .nombre(v.getNombre())
                .tipo(v.getTipo())
                .fechaAplicacion(v.getFechaAplicacion())
                .proximaDosis(v.getProximaDosis())
                .lote(v.getLote())
                .veterinario(v.getVeterinario())
                .notas(v.getNotas())
                .estadoAlerta(estadoAlerta)
                .diasParaVencimiento(dias)
                .build();
    }

    public EstadoVacuna calcularEstadoVacuna(LocalDate proximaDosis, LocalDate today) {
        if (proximaDosis == null) {
            return EstadoVacuna.COMPLETADA;
        }
        long dias = ChronoUnit.DAYS.between(today, proximaDosis);
        if (dias < 0) {
            return EstadoVacuna.VENCIDA;
        }
        if (dias <= DIAS_ALERTA_VACUNA) {
            return EstadoVacuna.PROXIMA;
        }
        return EstadoVacuna.AL_DIA;
    }
}
