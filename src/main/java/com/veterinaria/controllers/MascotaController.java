package com.veterinaria.controllers;

import com.veterinaria.dto.ExpedienteMascotaResponse;
import com.veterinaria.dto.MascotaRequest;
import com.veterinaria.dto.MascotaResponse;
import com.veterinaria.services.MascotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mascotas")
@RequiredArgsConstructor
@Tag(name = "Mascotas", description = "Gestión de pacientes veterinarios y expedientes clínicos")
public class MascotaController {

    private final MascotaService mascotaService;

    @GetMapping
    @Operation(summary = "Listar o filtrar mascotas por búsqueda, especie o propietario")
    public ResponseEntity<List<MascotaResponse>> obtenerTodas(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String especie,
            @RequestParam(required = false) Long propietarioId
    ) {
        return ResponseEntity.ok(mascotaService.obtenerTodas(q, especie, propietarioId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de una mascota")
    public ResponseEntity<MascotaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(mascotaService.obtenerPorId(id));
    }

    @GetMapping("/{id}/expediente")
    @Operation(summary = "Obtener expediente clínico completo de una mascota")
    public ResponseEntity<ExpedienteMascotaResponse> obtenerExpediente(@PathVariable Long id) {
        return ResponseEntity.ok(mascotaService.obtenerExpediente(id));
    }

    @GetMapping("/propietario/{propietarioId}")
    @Operation(summary = "Listar mascotas de un propietario")
    public ResponseEntity<List<MascotaResponse>> obtenerPorPropietario(@PathVariable Long propietarioId) {
        return ResponseEntity.ok(mascotaService.obtenerPorPropietario(propietarioId));
    }

    @PostMapping
    @Operation(summary = "Registrar nueva mascota")
    public ResponseEntity<MascotaResponse> crearMascota(@Valid @RequestBody MascotaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mascotaService.crearMascota(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar información de una mascota")
    public ResponseEntity<MascotaResponse> actualizarMascota(
            @PathVariable Long id,
            @Valid @RequestBody MascotaRequest request
    ) {
        return ResponseEntity.ok(mascotaService.actualizarMascota(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar mascota y su historial asociado")
    public ResponseEntity<Void> eliminarMascota(@PathVariable Long id) {
        mascotaService.eliminarMascota(id);
        return ResponseEntity.noContent().build();
    }
}
