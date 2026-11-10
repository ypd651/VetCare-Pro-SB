package com.veterinaria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veterinaria.dto.*;
import com.veterinaria.entities.EstadoCita;
import com.veterinaria.entities.PrioridadCita;
import com.veterinaria.entities.TipoCita;
import com.veterinaria.entities.TipoInmunizacion;
import com.veterinaria.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class VeterinariaApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConsultaMedicaRepository consultaRepository;

    @Autowired
    private VacunaRepository vacunaRepository;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private MascotaRepository mascotaRepository;

    @Autowired
    private PropietarioRepository propietarioRepository;

    @BeforeEach
    void limpiarBaseDeDatos() {
        consultaRepository.deleteAll();
        vacunaRepository.deleteAll();
        citaRepository.deleteAll();
        mascotaRepository.deleteAll();
        propietarioRepository.deleteAll();
    }

    @Test
    void flujoCompletoPropietarioYMascotaConValidaciones() throws Exception {
        // 1. Validacion 400 cuando faltan campos obligatorios o email es invalido
        PropietarioRequest invalidReq = PropietarioRequest.builder()
                .nombre("")
                .telefono("")
                .email("correo-invalido")
                .build();

        mockMvc.perform(post("/api/propietarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.nombre").exists())
                .andExpect(jsonPath("$.fieldErrors.telefono").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());

        // 2. Crear propietario valido
        PropietarioRequest propReq = PropietarioRequest.builder()
                .nombre("Laura Martínez")
                .documento("1010555999")
                .telefono("3109998877")
                .email("laura@correo.com")
                .direccion("Calle 10 # 20-30")
                .build();

        MvcResult propRes = mockMvc.perform(post("/api/propietarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(propReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Laura Martínez"))
                .andReturn();

        Long propietarioId = readId(propRes);

        // 3. Duplicado de correo debe devolver 409 Conflict
        PropietarioRequest dupEmailReq = PropietarioRequest.builder()
                .nombre("Otra Persona")
                .telefono("3001112233")
                .email("laura@correo.com")
                .build();

        mockMvc.perform(post("/api/propietarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupEmailReq)))
                .andExpect(status().isConflict());

        // 4. Crear mascota asociada
        MascotaRequest mascotaReq = MascotaRequest.builder()
                .nombre("Toby")
                .especie("Perro")
                .raza("Beagle")
                .sexo("Macho")
                .edadAnios(3)
                .pesoKg(11.2)
                .esterilizado(true)
                .propietarioId(propietarioId)
                .build();

        MvcResult mascotaRes = mockMvc.perform(post("/api/mascotas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mascotaReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Toby"))
                .andExpect(jsonPath("$.propietarioId").value(propietarioId))
                .andExpect(jsonPath("$.nombrePropietario").value("Laura Martínez"))
                .andReturn();

        Long mascotaId = readId(mascotaRes);

        // 5. Actualizar mascota via PUT
        mascotaReq.setPesoKg(11.8);
        mascotaReq.setAlergias("Ninguna conocida");
        mockMvc.perform(put("/api/mascotas/" + mascotaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mascotaReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pesoKg").value(11.8))
                .andExpect(jsonPath("$.alergias").value("Ninguna conocida"));

        // 6. Buscar propietario por query
        mockMvc.perform(get("/api/propietarios").param("q", "Laura"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cantidadMascotas").value(1));

        // 7. Consultar recurso inexistente devuelve 404
        mockMvc.perform(get("/api/mascotas/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void agendaCitasDetectaConflictosYConsultaCompletaCitaYActualizaPeso() throws Exception {
        Long propId = crearPropietario("Carlos Ruiz", "3112223344", "carlos@correo.com");
        Long mascota1Id = crearMascota("Nala", "Gato", propId, 3.5);
        Long mascota2Id = crearMascota("Bruno", "Perro", propId, 18.0);

        LocalDateTime fechaCita = LocalDate.now().atTime(10, 0);

        CitaRequest cita1 = CitaRequest.builder()
                .mascotaId(mascota1Id)
                .fechaHora(fechaCita)
                .tipo(TipoCita.CONSULTA_GENERAL)
                .prioridad(PrioridadCita.ALTA)
                .veterinario("Dra. Ana Pérez")
                .motivo("Revisión general")
                .build();

        MvcResult citaRes = mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cita1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"))
                .andReturn();

        Long citaId = readId(citaRes);

        // Conflicto: misma veterinaria 15 minutos despues
        CitaRequest citaConflicto = CitaRequest.builder()
                .mascotaId(mascota2Id)
                .fechaHora(fechaCita.plusMinutes(15))
                .tipo(TipoCita.VACUNACION)
                .veterinario("Dra. Ana Pérez")
                .motivo("Vacuna")
                .build();

        mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaConflicto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Conflicto de agenda")));

        // Pasar cita a EN_ATENCION
        mockMvc.perform(patch("/api/citas/" + citaId + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CitaEstadoRequest(EstadoCita.EN_ATENCION))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_ATENCION"));

        // Registrar consulta medica vinculada a la cita -> debe actualizar peso y completar la cita
        ConsultaMedicaRequest consultaReq = ConsultaMedicaRequest.builder()
                .mascotaId(mascota1Id)
                .citaId(citaId)
                .veterinario("Dra. Ana Pérez")
                .motivoConsulta("Control general")
                .diagnostico("Paciente estable")
                .tratamiento("Vitaminas por 10 días")
                .pesoKg(3.9)
                .temperaturaC(38.5)
                .proximaRevision(LocalDate.now().plusDays(7))
                .build();

        mockMvc.perform(post("/api/consultas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(consultaReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pesoKg").value(3.9));

        // Verificar que la cita paso a COMPLETADA
        mockMvc.perform(get("/api/citas/" + citaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));

        // Verificar que el peso de la mascota se actualizo automaticamente
        mockMvc.perform(get("/api/mascotas/" + mascota1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pesoKg").value(3.9))
                .andExpect(jsonPath("$.totalConsultas").value(1));

        // Verificar que eliminar la cita no falla aunque tenga una consulta asociada
        mockMvc.perform(delete("/api/citas/" + citaId))
                .andExpect(status().isNoContent());
    }

    @Test
    void vacunasCalculaAlertasYDashboardConsolidaMetricas() throws Exception {
        Long propId = crearPropietario("Sofía Castro", "3184445566", "sofia@correo.com");
        Long mascotaId = crearMascota("Milo", "Perro", propId, 9.5);

        // Vacuna vencida
        VacunaRequest vencida = VacunaRequest.builder()
                .mascotaId(mascotaId)
                .nombre("Rabia")
                .tipo(TipoInmunizacion.VACUNA)
                .fechaAplicacion(LocalDate.now().minusYears(1).minusDays(3))
                .proximaDosis(LocalDate.now().minusDays(3))
                .veterinario("Dr. Gómez")
                .build();

        // Vacuna proxima (en 5 dias)
        VacunaRequest proxima = VacunaRequest.builder()
                .mascotaId(mascotaId)
                .nombre("Séxtuple")
                .tipo(TipoInmunizacion.VACUNA)
                .fechaAplicacion(LocalDate.now().minusMonths(11).minusDays(25))
                .proximaDosis(LocalDate.now().plusDays(5))
                .veterinario("Dr. Gómez")
                .build();

        // Desparasitacion al dia (en 60 dias)
        VacunaRequest alDia = VacunaRequest.builder()
                .mascotaId(mascotaId)
                .nombre("Antiparasitario")
                .tipo(TipoInmunizacion.DESPARASITACION)
                .fechaAplicacion(LocalDate.now().minusDays(30))
                .proximaDosis(LocalDate.now().plusDays(60))
                .build();

        mockMvc.perform(post("/api/vacunas").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vencida)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoAlerta").value("VENCIDA"));

        mockMvc.perform(post("/api/vacunas").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(proxima)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoAlerta").value("PROXIMA"));

        mockMvc.perform(post("/api/vacunas").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alDia)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoAlerta").value("AL_DIA"));

        // Filtrar solo alertas (VENCIDA + PROXIMA)
        mockMvc.perform(get("/api/vacunas").param("estado", "ALERTAS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Verificar expediente completo
        mockMvc.perform(get("/api/mascotas/" + mascotaId + "/expediente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mascota.nombre").value("Milo"))
                .andExpect(jsonPath("$.propietario.nombre").value("Sofía Castro"))
                .andExpect(jsonPath("$.vacunas", hasSize(3)));

        // Verificar dashboard consolidado
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPropietarios").value(1))
                .andExpect(jsonPath("$.totalMascotas").value(1))
                .andExpect(jsonPath("$.vacunasVencidas").value(1))
                .andExpect(jsonPath("$.vacunasProximas").value(1))
                .andExpect(jsonPath("$.alertasVacunas", hasSize(2)));
    }

    @Test
    void frontendYActuatorDisponibles() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("VetCare Pro")));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("VetCare Pro - API Clínica Veterinaria"));
    }

    private Long crearPropietario(String nombre, String telefono, String email) throws Exception {
        PropietarioRequest req = PropietarioRequest.builder()
                .nombre(nombre)
                .telefono(telefono)
                .email(email)
                .build();
        MvcResult result = mockMvc.perform(post("/api/propietarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
    }

    private Long crearMascota(String nombre, String especie, Long propietarioId, Double pesoKg) throws Exception {
        MascotaRequest req = MascotaRequest.builder()
                .nombre(nombre)
                .especie(especie)
                .pesoKg(pesoKg)
                .propietarioId(propietarioId)
                .build();
        MvcResult result = mockMvc.perform(post("/api/mascotas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
    }

    private Long readId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
