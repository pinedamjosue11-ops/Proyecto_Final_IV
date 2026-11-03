package com.example.proyecto_final_iv.service;

import com.example.proyecto_final_iv.dto.*;
import com.example.proyecto_final_iv.model.Categoria;
import com.example.proyecto_final_iv.model.Producto;
import com.example.proyecto_final_iv.repository.ICategoriaRepository;
import com.example.proyecto_final_iv.repository.iProductoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private final iProductoRepository productoRepository;
    private final ICategoriaRepository categoriaRepository;

    // Inyección de dependencias por constructor
    public ProductoService(iProductoRepository productoRepository, ICategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    // Consulta paginada con filtro opcional por nombre o código
    public Page<Producto> listarProductos(String query, Pageable pageable) {
        if (query != null && !query.isBlank()) {
            return productoRepository.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(query, query, pageable);
        }
        return productoRepository.findAll(pageable);
    }

    // Obtener un producto individual por ID
    public Optional<Producto> obtenerPorId(Long id) {
        return productoRepository.findById(id);
    }

    // Listar todas las categorías disponibles
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }

    // Endpoint estrella: Procesamiento por lote que llamará el microservicio de Importaciones
    @Transactional
    public LoteResultDto procesarLote(List<ProductoRequestDto> items) {
        List<ProductoResultDto> resultados = new ArrayList<>();

        for (ProductoRequestDto item : items) {
            // Regla 1: Validar campos obligatorios
            if (item.codigo() == null || item.codigo().isBlank() ||
                    item.nombre() == null || item.nombre().isBlank() ||
                    item.categoria() == null || item.categoria().isBlank()) {
                resultados.add(new ProductoResultDto(item.numeroFila(), item.codigo(), false, "Campos obligatorios incompletos"));
                continue;
            }

            // Regla 2: Precio > 0 y Existencia >= 0
            if (item.precio() == null || item.precio().compareTo(BigDecimal.ZERO) <= 0) {
                resultados.add(new ProductoResultDto(item.numeroFila(), item.codigo(), false, "El precio debe ser mayor a 0"));
                continue;
            }
            if (item.existencia() == null || item.existencia() < 0) {
                resultados.add(new ProductoResultDto(item.numeroFila(), item.codigo(), false, "La existencia debe ser mayor o igual a 0"));
                continue;
            }

            // Regla 3: Verificar que el código no exista previamente en el catálogo
            if (productoRepository.existsByCodigo(item.codigo())) {
                resultados.add(new ProductoResultDto(item.numeroFila(), item.codigo(), false, "El código del producto ya existe"));
                continue;
            }

            // Regla 4: Validar existencia de la categoría especificada
            Optional<Categoria> catOpt = categoriaRepository.findByNombreIgnoreCase(item.categoria());
            if (catOpt.isEmpty()) {
                resultados.add(new ProductoResultDto(item.numeroFila(), item.codigo(), false, "La categoría '" + item.categoria() + "' no existe en el catálogo"));
                continue;
            }

            // Si pasa todas las validaciones de negocio, persistir
            Producto producto = new Producto();
            producto.setCodigo(item.codigo());
            producto.setNombre(item.nombre());
            producto.setCategoria(catOpt.get());
            producto.setPrecio(item.precio());
            producto.setExistencia(item.existencia());
            productoRepository.save(producto);

            resultados.add(new ProductoResultDto(item.numeroFila(), item.codigo(), true, null));
        }

        return new LoteResultDto(resultados);
    }
}