package com.example.proyecto_final_iv.controller;

import com.example.proyecto_final_iv.dto.LoteResultDto;
import com.example.proyecto_final_iv.model.Categoria;
import com.example.proyecto_final_iv.model.Producto;
import com.example.proyecto_final_iv.service.ProductoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.proyecto_final_iv.dto.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // 1. GET /api/v1/productos?query=...&page=0&size=10 (Consulta paginada y con filtro)
    @GetMapping
    public ResponseEntity<Page<Producto>> listar(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Producto> productos = productoService.listarProductos(query, PageRequest.of(page, size));
        return ResponseEntity.ok(productos);
    }

    // 2. GET /api/v1/productos/{id} (Detalle de un producto)
    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerPorId(@PathVariable Long id) {
        return productoService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. GET /api/v1/productos/categorias (Listado de categorías disponibles)
    @GetMapping("/categorias")
    public ResponseEntity<List<Categoria>> listarCategorias() {
        return ResponseEntity.ok(productoService.listarCategorias());
    }

    // 4. POST /api/v1/productos/lote (Registro en lote invocado por Importaciones)
    @PostMapping("/lote")
    public ResponseEntity<LoteResultDto> registrarLote(@RequestBody List<ProductoRequestDto> lote) {
        LoteResultDto resultado = productoService.procesarLote(lote);
        return ResponseEntity.ok(resultado);
    }
}