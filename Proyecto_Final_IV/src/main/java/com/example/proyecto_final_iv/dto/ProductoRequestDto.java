package com.example.proyecto_final_iv.dto;

import java.math.BigDecimal;

public record ProductoRequestDto(
        int numeroFila,
        String codigo,
        String nombre,
        String categoria,
        BigDecimal precio,
        Integer existencia
) {}