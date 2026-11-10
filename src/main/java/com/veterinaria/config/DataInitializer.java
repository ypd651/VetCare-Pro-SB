package com.veterinaria.config;

import com.veterinaria.entities.*;
import com.veterinaria.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PropietarioRepository propietarioRepository;
    private final MascotaRepository mascotaRepository;
    private final CitaRepository citaRepository;
    private final ConsultaMedicaRepository consultaRepository;
    private final VacunaRepository vacunaRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (propietarioRepository.count() > 0) {
            return;
        }

        Propietario camila = propietarioRepository.save(Propietario.builder()
                .nombre("Camila Mendoza")
                .documento("1020458912")
                .telefono("+57 310 458 9210")
                .email("camila.mendoza@correo.com")
                .direccion("Calle 45 # 18-32, Chapinero")
                .notas("Prefiere recordatorios por WhatsApp en horario de la mañana.")
                .build());

        Propietario andres = propietarioRepository.save(Propietario.builder()
                .nombre("Andrés Felipe Rojas")
                .documento("79845123")
                .telefono("+57 315 742 3309")
                .email("andres.rojas@correo.com")
                .direccion("Av. El Poblado # 12-90")
                .notas("Cliente frecuente con dos mascotas.")
                .build());

        Propietario valentina = propietarioRepository.save(Propietario.builder()
                .nombre("Valentina Gómez")
                .documento("1032487510")
                .telefono("+57 320 912 4481")
                .email("valentina.gomez@correo.com")
                .direccion("Carrera 7 # 112-15")
                .build());

        Propietario santiago = propietarioRepository.save(Propietario.builder()
                .nombre("Santiago Morales")
                .documento("80119452")
                .telefono("+57 301 664 1890")
                .email("santiago.morales@correo.com")
                .direccion("Calle 80 # 24-11")
                .build());

        Mascota max = mascotaRepository.save(Mascota.builder()
                .nombre("Max")
                .especie("Perro")
                .raza("Golden Retriever")
                .sexo("Macho")
                .edadAnios(4)
                .pesoKg(29.5)
                .color("Dorado")
                .microchip("MC-900118204")
                .esterilizado(true)
                .alergias("Sensibilidad leve al pollo procesado")
                .notasMedicas("Paciente dócil, buena respuesta a vacunas.")
                .propietario(camila)
                .build());

        Mascota luna = mascotaRepository.save(Mascota.builder()
                .nombre("Luna")
                .especie("Gato")
                .raza("Siamés")
                .sexo("Hembra")
                .edadAnios(2)
                .pesoKg(3.8)
                .color("Crema y chocolate")
                .microchip("MC-900118599")
                .esterilizado(true)
                .notasMedicas("Nerviosa en sala de espera, atender en consultorio tranquilo.")
                .propietario(andres)
                .build());

        Mascota rocky = mascotaRepository.save(Mascota.builder()
                .nombre("Rocky")
                .especie("Perro")
                .raza("Bulldog Francés")
                .sexo("Macho")
                .edadAnios(5)
                .pesoKg(12.4)
                .color("Atigrado")
                .esterilizado(false)
                .alergias("Alergia estacional al polen")
                .notasMedicas("Vigilar respiración en días calurosos.")
                .propietario(andres)
                .build());

        Mascota kiwi = mascotaRepository.save(Mascota.builder()
                .nombre("Kiwi")
                .especie("Ave")
                .raza("Periquito Australiano")
                .sexo("Hembra")
                .edadAnios(1)
                .pesoKg(0.04)
                .color("Verde y amarillo")
                .esterilizado(false)
                .propietario(valentina)
                .build());

        Mascota copo = mascotaRepository.save(Mascota.builder()
                .nombre("Copo")
                .especie("Conejo")
                .raza("Mini Lop")
                .sexo("Macho")
                .edadAnios(3)
                .pesoKg(2.1)
                .color("Blanco")
                .esterilizado(true)
                .propietario(santiago)
                .build());

        LocalDate today = LocalDate.now();
        LocalDateTime hoyBase = today.atTime(9, 0);

        Cita citaUrgente = citaRepository.save(Cita.builder()
                .mascota(rocky)
                .fechaHora(hoyBase.plusHours(1))
                .tipo(TipoCita.URGENCIA)
                .prioridad(PrioridadCita.URGENTE)
                .estado(EstadoCita.EN_ATENCION)
                .veterinario("Dra. Laura Restrepo")
                .motivo("Dificultad respiratoria leve tras paseo matutino")
                .notas("Revisar vías aéreas y temperatura inmediatamente.")
                .build());

        Cita citaHoy2 = citaRepository.save(Cita.builder()
                .mascota(max)
                .fechaHora(hoyBase.plusHours(3))
                .tipo(TipoCita.VACUNACION)
                .prioridad(PrioridadCita.ALTA)
                .estado(EstadoCita.PROGRAMADA)
                .veterinario("Dr. Carlos Ramírez")
                .motivo("Refuerzo anual de vacuna antirrábica")
                .build());

        Cita citaHoy3 = citaRepository.save(Cita.builder()
                .mascota(luna)
                .fechaHora(hoyBase.plusHours(5))
                .tipo(TipoCita.CONTROL)
                .prioridad(PrioridadCita.NORMAL)
                .estado(EstadoCita.PROGRAMADA)
                .veterinario("Dra. Laura Restrepo")
                .motivo("Control post-tratamiento dermatológico")
                .build());

        Cita citaManana = citaRepository.save(Cita.builder()
                .mascota(copo)
                .fechaHora(hoyBase.plusDays(1).plusHours(2))
                .tipo(TipoCita.CONSULTA_GENERAL)
                .prioridad(PrioridadCita.NORMAL)
                .estado(EstadoCita.PROGRAMADA)
                .veterinario("Dr. Carlos Ramírez")
                .motivo("Chequeo dental preventivo y corte de uñas")
                .build());

        Cita citaPasada = citaRepository.save(Cita.builder()
                .mascota(max)
                .fechaHora(hoyBase.minusDays(12))
                .tipo(TipoCita.CONSULTA_GENERAL)
                .prioridad(PrioridadCita.NORMAL)
                .estado(EstadoCita.COMPLETADA)
                .veterinario("Dra. Laura Restrepo")
                .motivo("Chequeo general y desparasitación")
                .build());

        consultaRepository.saveAll(List.of(
                ConsultaMedica.builder()
                        .mascota(max)
                        .cita(citaPasada)
                        .fechaConsulta(hoyBase.minusDays(12))
                        .veterinario("Dra. Laura Restrepo")
                        .motivoConsulta("Chequeo general anual y control de peso")
                        .sintomas("Sin síntomas anormales, buen apetito y energía.")
                        .diagnostico("Estado clínico saludable; leve sobrepeso de 0.5 kg.")
                        .tratamiento("Ajuste de porción diaria de alimento y suplemento articular preventivo.")
                        .pesoKg(29.5)
                        .temperaturaC(38.4)
                        .frecuenciaCardiaca(88)
                        .proximaRevision(today.plusDays(10))
                        .observaciones("Programar refuerzo de vacuna antirrábica.")
                        .build(),
                ConsultaMedica.builder()
                        .mascota(luna)
                        .fechaConsulta(hoyBase.minusDays(7))
                        .veterinario("Dra. Laura Restrepo")
                        .motivoConsulta("Prurito leve en oreja izquierda")
                        .sintomas("Rascado frecuente y leve enrojecimiento auricular.")
                        .diagnostico("Otitis externa leve de origen alérgico.")
                        .tratamiento("Limpieza ótica cada 48h y gotas óticas antiinflamatorias por 7 días.")
                        .pesoKg(3.8)
                        .temperaturaC(38.6)
                        .frecuenciaCardiaca(140)
                        .proximaRevision(today.plusDays(3))
                        .observaciones("Control de evolución en una semana.")
                        .build(),
                ConsultaMedica.builder()
                        .mascota(kiwi)
                        .fechaConsulta(hoyBase.minusDays(4))
                        .veterinario("Dr. Carlos Ramírez")
                        .motivoConsulta("Revisión de plumaje y nutrición")
                        .sintomas("Plumaje sano, pico en buen estado.")
                        .diagnostico("Paciente exótico en óptimas condiciones.")
                        .tratamiento("Continuar dieta balanceada con semillas y vegetales frescos.")
                        .pesoKg(0.04)
                        .observaciones("Sin anomalías respiratorias.")
                        .build()
        ));

        vacunaRepository.saveAll(List.of(
                Vacuna.builder()
                        .mascota(max)
                        .nombre("Antirrábica Anual")
                        .tipo(TipoInmunizacion.VACUNA)
                        .fechaAplicacion(today.minusMonths(12).minusDays(4))
                        .proximaDosis(today.minusDays(4))
                        .lote("RAB-2025-09")
                        .veterinario("Dr. Carlos Ramírez")
                        .notas("Requiere contacto urgente con propietario para refuerzo.")
                        .build(),
                Vacuna.builder()
                        .mascota(rocky)
                        .nombre("Polivalente Séxtuple Canina")
                        .tipo(TipoInmunizacion.VACUNA)
                        .fechaAplicacion(today.minusMonths(12).minusDays(10))
                        .proximaDosis(today.minusDays(10))
                        .lote("SEX-2025-44")
                        .veterinario("Dra. Laura Restrepo")
                        .notas("Dosis vencida; aplicar apenas estabilice cuadro respiratorio.")
                        .build(),
                Vacuna.builder()
                        .mascota(luna)
                        .nombre("Triple Felina")
                        .tipo(TipoInmunizacion.VACUNA)
                        .fechaAplicacion(today.minusMonths(11).minusDays(20))
                        .proximaDosis(today.plusDays(6))
                        .lote("TRI-2025-18")
                        .veterinario("Dra. Laura Restrepo")
                        .notas("Recordar en la cita de control.")
                        .build(),
                Vacuna.builder()
                        .mascota(copo)
                        .nombre("Desparasitación Interna Preventiva")
                        .tipo(TipoInmunizacion.DESPARASITACION)
                        .fechaAplicacion(today.minusMonths(3))
                        .proximaDosis(today.plusDays(12))
                        .lote("DESP-441")
                        .veterinario("Dr. Carlos Ramírez")
                        .build(),
                Vacuna.builder()
                        .mascota(max)
                        .nombre("Desparasitación Interna Trimestral")
                        .tipo(TipoInmunizacion.DESPARASITACION)
                        .fechaAplicacion(today.minusDays(12))
                        .proximaDosis(today.plusDays(78))
                        .lote("DESP-889")
                        .veterinario("Dra. Laura Restrepo")
                        .build()
        ));

        log.info("Datos iniciales de demostración cargados correctamente (Propietarios: 4, Mascotas: 5, Citas: 5).");
    }
}
