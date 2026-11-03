package com.example.importacionesservice.repository;

import com.example.importacionesservice.model.ImportacionError;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ImportacionErrorRepository extends JpaRepository<ImportacionError, Long> {
    List<ImportacionError> findByImportacionIdOrderByNumeroFilaAsc(Long importacionId);
}