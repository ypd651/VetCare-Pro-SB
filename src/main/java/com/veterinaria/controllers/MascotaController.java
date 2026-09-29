package com.veterinaria.controllers;

import com.veterinaria.dto.CreateMascotaRequest;
import com.veterinaria.dto.MascotaResponse;
import com.veterinaria.servicies.MascotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mascotas")
@RequiredArgsConstructor
public class MascotaController {

    private final MascotaService mascotaService;

    @GetMapping
    public ResponseEntity<List<MascotaResponse>> obtenerTodas() {
        return ResponseEntity.ok(mascotaService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MascotaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(mascotaService.obtenerPorId(id));
    }

    @GetMapping("/propietario/{propietarioId}")
    public ResponseEntity<List<MascotaResponse>> obtenerPorPropietario(@PathVariable Long propietarioId) {
        return ResponseEntity.ok(mascotaService.obtenerPorPropietario(propietarioId));
    }

    @PostMapping
    public ResponseEntity<MascotaResponse> crearMascota(@RequestBody CreateMascotaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mascotaService.crearMascota(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarMascota(@PathVariable Long id) {
        mascotaService.eliminarMascota(id);
        return ResponseEntity.noContent().build();
    }
}