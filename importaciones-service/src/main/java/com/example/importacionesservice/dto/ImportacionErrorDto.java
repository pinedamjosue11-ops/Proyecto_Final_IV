package com.example.importacionesservice.dto;

public record ImportacionErrorDto(
        int numeroFila,
        String datosRecibidos,
        String motivo
) {}
