package com.example.importacionesservice.dto;

import java.util.List;

public record LoteResultDto(
        List<ProductoResultDto> resultados
) {}