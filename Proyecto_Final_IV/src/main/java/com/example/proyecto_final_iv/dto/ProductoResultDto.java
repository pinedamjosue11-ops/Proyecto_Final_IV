package com.example.proyecto_final_iv.dto;

public record ProductoResultDto(
        int numeroFila,
        String codigo,
        boolean exitoso,
        String motivoError
) {}