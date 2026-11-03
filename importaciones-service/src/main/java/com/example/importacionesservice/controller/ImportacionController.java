package com.example.importacionesservice.controller;

import com.example.importacionesservice.dto.ImportacionResumenDto;
import com.example.importacionesservice.service.ImportacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/importaciones")
public class ImportacionController {

    private final ImportacionService importacionService;

    public ImportacionController(ImportacionService importacionService) {
        this.importacionService = importacionService;
    }

    @PostMapping("/cargar")
    public ResponseEntity<ImportacionResumenDto> cargarCsv(@RequestParam("archivo") MultipartFile archivo) {
        ImportacionResumenDto resumen = importacionService.procesarArchivoCsv(archivo);
        return ResponseEntity.ok(resumen);
    }
}