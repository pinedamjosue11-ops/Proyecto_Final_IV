package com.example.proyecto_final_iv.repository;

import com.example.proyecto_final_iv.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface iProductoRepository extends JpaRepository<Producto, Long> {
    boolean existsByCodigo(String codigo);

    @Query("""
            SELECT p FROM Producto p
            WHERE (:query IS NULL OR :query = ''
                OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:categoria IS NULL OR :categoria = ''
                OR LOWER(p.categoria.nombre) = LOWER(:categoria))
            """)
    Page<Producto> buscar(
            @Param("query") String query,
            @Param("categoria") String categoria,
            Pageable pageable
    );
}