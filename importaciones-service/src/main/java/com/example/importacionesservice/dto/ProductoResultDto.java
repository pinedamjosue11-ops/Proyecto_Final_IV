package com.example.importacionesservice.dto;

public record ProductoResultDto(
        int numeroFila,
        String codigo,
        boolean exitoso,
        String motivoError
) {}