package com.example.importacionesservice.dto;

import java.math.BigDecimal;

public record ProductoItemDto(
        int numeroFila,
        String codigo,
        String nombre,
        String categoria,
        BigDecimal precio,
        Integer existencia
) {}