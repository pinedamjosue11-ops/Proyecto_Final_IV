package com.example.importacionesservice.dto;

import java.util.List;

public record ImportacionDetalleDto(
        ImportacionResumenDto importacion,
        List<ImportacionErrorDto> errores
) {}
