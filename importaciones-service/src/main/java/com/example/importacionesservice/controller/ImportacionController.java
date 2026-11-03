package com.example.importacionesservice.controller;

import com.example.importacionesservice.dto.ImportacionResumenDto;
import com.example.importacionesservice.dto.ImportacionDetalleDto;
import com.example.importacionesservice.dto.ImportacionErrorDto;
import com.example.importacionesservice.service.ImportacionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/importaciones")
public class ImportacionController {

    private final ImportacionService importacionService;

    public ImportacionController(ImportacionService importacionService) {
        this.importacionService = importacionService;
    }

    @GetMapping
    public Page<ImportacionResumenDto> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return importacionService.listarImportaciones(PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImportacionDetalleDto> obtener(@PathVariable Long id) {
        return importacionService.obtenerImportacion(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/errores")
    public ResponseEntity<List<ImportacionErrorDto>> listarErrores(@PathVariable Long id) {
        return importacionService.listarErrores(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/cargar")
    public ResponseEntity<ImportacionResumenDto> cargarCsv(@RequestParam("archivo") MultipartFile archivo) {
        ImportacionResumenDto resumen = importacionService.procesarArchivoCsv(archivo);
        return ResponseEntity.ok(resumen);
    }
}