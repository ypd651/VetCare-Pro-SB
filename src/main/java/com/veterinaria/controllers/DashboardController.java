package com.veterinaria.controllers;

import com.veterinaria.dto.DashboardResponse;
import com.veterinaria.services.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Indicadores operativos, alertas sanitarias y agenda del día")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Obtener resumen operativo de la clínica")
    public ResponseEntity<DashboardResponse> obtenerResumen() {
        return ResponseEntity.ok(dashboardService.obtenerResumen());
    }
}
