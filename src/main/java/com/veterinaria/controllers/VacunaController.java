package com.veterinaria.controllers;

import com.veterinaria.dto.VacunaRequest;
import com.veterinaria.dto.VacunaResponse;
import com.veterinaria.entities.TipoInmunizacion;
import com.veterinaria.services.VacunaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vacunas")
@RequiredArgsConstructor
@Tag(name = "Vacunas y Desparasitación", description = "Control preventivo con cálculo automático de alertas de vencimiento")
public class VacunaController {

    private final VacunaService vacunaService;

    @GetMapping
    @Operation(summary = "Listar vacunas y desparasitaciones con filtro de alertas")
    public ResponseEntity<List<VacunaResponse>> obtenerTodas(
            @RequestParam(required = false) Long mascotaId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) TipoInmunizacion tipo,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(vacunaService.obtenerTodas(mascotaId, estado, tipo, q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener registro de vacuna por ID")
    public ResponseEntity<VacunaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(vacunaService.obtenerPorId(id));
    }

    @PostMapping
    @Operation(summary = "Registrar aplicación de vacuna o desparasitación")
    public ResponseEntity<VacunaResponse> registrarVacuna(@Valid @RequestBody VacunaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vacunaService.registrarVacuna(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar registro de vacunación")
    public ResponseEntity<VacunaResponse> actualizarVacuna(
            @PathVariable Long id,
            @Valid @RequestBody VacunaRequest request
    ) {
        return ResponseEntity.ok(vacunaService.actualizarVacuna(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar registro de vacunación")
    public ResponseEntity<Void> eliminarVacuna(@PathVariable Long id) {
        vacunaService.eliminarVacuna(id);
        return ResponseEntity.noContent().build();
    }
}
