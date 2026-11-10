package com.veterinaria.controllers;

import com.veterinaria.dto.ConsultaMedicaRequest;
import com.veterinaria.dto.ConsultaMedicaResponse;
import com.veterinaria.services.ConsultaMedicaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consultas")
@RequiredArgsConstructor
@Tag(name = "Consultas Médicas", description = "Registro de atenciones clínicas, diagnósticos, tratamientos y signos vitales")
public class ConsultaMedicaController {

    private final ConsultaMedicaService consultaService;

    @GetMapping
    @Operation(summary = "Listar historial de consultas médicas")
    public ResponseEntity<List<ConsultaMedicaResponse>> obtenerTodas(
            @RequestParam(required = false) Long mascotaId,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(consultaService.obtenerTodas(mascotaId, q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener consulta médica por ID")
    public ResponseEntity<ConsultaMedicaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(consultaService.obtenerPorId(id));
    }

    @PostMapping
    @Operation(summary = "Registrar consulta médica (actualiza peso del paciente y completa cita asociada)")
    public ResponseEntity<ConsultaMedicaResponse> crearConsulta(@Valid @RequestBody ConsultaMedicaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consultaService.crearConsulta(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar consulta médica")
    public ResponseEntity<ConsultaMedicaResponse> actualizarConsulta(
            @PathVariable Long id,
            @Valid @RequestBody ConsultaMedicaRequest request
    ) {
        return ResponseEntity.ok(consultaService.actualizarConsulta(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar consulta médica")
    public ResponseEntity<Void> eliminarConsulta(@PathVariable Long id) {
        consultaService.eliminarConsulta(id);
        return ResponseEntity.noContent().build();
    }
}
