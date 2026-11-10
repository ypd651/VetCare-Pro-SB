package com.veterinaria.controllers;

import com.veterinaria.dto.PropietarioRequest;
import com.veterinaria.dto.PropietarioResponse;
import com.veterinaria.services.PropietarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/propietarios")
@RequiredArgsConstructor
@Tag(name = "Propietarios", description = "Gestión de dueños de mascotas y datos de contacto")
public class PropietarioController {

    private final PropietarioService propietarioService;

    @GetMapping
    @Operation(summary = "Listar o buscar propietarios")
    public ResponseEntity<List<PropietarioResponse>> obtenerTodos(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(propietarioService.obtenerTodos(q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener propietario por ID")
    public ResponseEntity<PropietarioResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(propietarioService.obtenerPorId(id));
    }

    @PostMapping
    @Operation(summary = "Registrar un nuevo propietario")
    public ResponseEntity<PropietarioResponse> crearPropietario(@Valid @RequestBody PropietarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(propietarioService.crearPropietario(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un propietario existente")
    public ResponseEntity<PropietarioResponse> actualizarPropietario(
            @PathVariable Long id,
            @Valid @RequestBody PropietarioRequest request
    ) {
        return ResponseEntity.ok(propietarioService.actualizarPropietario(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar propietario y sus mascotas asociadas")
    public ResponseEntity<Void> eliminarPropietario(@PathVariable Long id) {
        propietarioService.eliminarPropietario(id);
        return ResponseEntity.noContent().build();
    }
}
