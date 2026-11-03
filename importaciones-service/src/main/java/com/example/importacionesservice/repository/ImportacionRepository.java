package com.example.importacionesservice.repository;

import com.example.importacionesservice.model.Importacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportacionRepository extends JpaRepository<Importacion, Long> {
}