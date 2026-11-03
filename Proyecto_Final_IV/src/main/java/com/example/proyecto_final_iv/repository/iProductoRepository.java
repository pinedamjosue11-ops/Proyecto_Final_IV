package com.example.proyecto_final_iv.repository;

import com.example.proyecto_final_iv.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface iProductoRepository extends JpaRepository<Producto, Long> {
    boolean existsByCodigo(String codigo);

    // Consulta con búsqueda por filtro y paginación vista en clase
    Page<Producto> findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(
            String nombre, String codigo, Pageable pageable
    );
}