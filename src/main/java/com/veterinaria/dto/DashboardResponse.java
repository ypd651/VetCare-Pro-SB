package com.veterinaria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private long totalPropietarios;
    private long totalMascotas;
    private long citasHoy;
    private long citasPendientes;
    private long urgenciasActivas;
    private long vacunasVencidas;
    private long vacunasProximas;
    private long consultasMes;

    private List<EspecieStat> distribucionEspecies;
    private List<CitaResponse> citasHoyLista;
    private List<VacunaResponse> alertasVacunas;
    private List<ConsultaMedicaResponse> proximasRevisiones;
    private List<ConsultaMedicaResponse> consultasRecientes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EspecieStat {
        private String especie;
        private long cantidad;
    }
}
