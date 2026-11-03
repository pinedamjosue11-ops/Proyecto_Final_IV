package com.example.importacionesservice.repository;

import com.example.importacionesservice.model.Importacion;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

public interface ImportacionRepository extends JpaRepository<Importacion, Long> {
    Page<Importacion> findAllByOrderByFechaProcesoDesc(Pageable pageable);
}