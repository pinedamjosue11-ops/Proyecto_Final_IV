package com.example.importacionesservice.dto;

import java.time.LocalDateTime;

public record ImportacionResumenDto(
        Long id,
        String nombreArchivo,
        LocalDateTime fechaProceso,
        int totalFilas,
        int aceptados,
        int rechazados,
        String estado
) {}