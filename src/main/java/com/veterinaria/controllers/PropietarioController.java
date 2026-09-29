package com.veterinaria.controllers;

import com.veterinaria.dto.CreatePropietarioRequest;
import com.veterinaria.dto.PropietarioResponse;
import com.veterinaria.servicies.PropietarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/propietarios")
@RequiredArgsConstructor
public class PropietarioController {

    private final PropietarioService propietarioService;

    @GetMapping
    public ResponseEntity<List<PropietarioResponse>> obtenerTodos() {
        return ResponseEntity.ok(propietarioService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropietarioResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(propietarioService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<PropietarioResponse> crearPropietario(@RequestBody CreatePropietarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(propietarioService.crearPropietario(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPropietario(@PathVariable Long id) {
        propietarioService.eliminarPropietario(id);
        return ResponseEntity.noContent().build();
    }
}