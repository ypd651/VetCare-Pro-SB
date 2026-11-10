package com.veterinaria.controllers;

import com.veterinaria.dto.CitaEstadoRequest;
import com.veterinaria.dto.CitaRequest;
import com.veterinaria.dto.CitaResponse;
import com.veterinaria.entities.EstadoCita;
import com.veterinaria.entities.PrioridadCita;
import com.veterinaria.services.CitaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
@Tag(name = "Citas", description = "Agenda veterinaria con detección de conflictos y control de triage")
public class CitaController {

    private final CitaService citaService;

    @GetMapping
    @Operation(summary = "Listar o filtrar citas por estado, prioridad, fecha ('hoy', 'proximas', 'YYYY-MM-DD') o búsqueda")
    public ResponseEntity<List<CitaResponse>> obtenerTodas(
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false) PrioridadCita prioridad,
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false) Long mascotaId,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(citaService.obtenerTodas(estado, prioridad, fecha, mascotaId, q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una cita por ID")
    public ResponseEntity<CitaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.obtenerPorId(id));
    }

    @PostMapping
    @Operation(summary = "Programar nueva cita (valida conflictos de horario)")
    public ResponseEntity<CitaResponse> crearCita(@Valid @RequestBody CitaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.crearCita(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reprogramar o editar cita")
    public ResponseEntity<CitaResponse> actualizarCita(
            @PathVariable Long id,
            @Valid @RequestBody CitaRequest request
    ) {
        return ResponseEntity.ok(citaService.actualizarCita(id, request));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado de atención de una cita")
    public ResponseEntity<CitaResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CitaEstadoRequest request
    ) {
        return ResponseEntity.ok(citaService.cambiarEstado(id, request.getEstado()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar cita")
    public ResponseEntity<Void> eliminarCita(@PathVariable Long id) {
        citaService.eliminarCita(id);
        return ResponseEntity.noContent().build();
    }
}
