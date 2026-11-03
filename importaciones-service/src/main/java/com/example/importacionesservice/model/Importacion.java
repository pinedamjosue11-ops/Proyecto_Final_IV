package com.example.importacionesservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "importaciones")
public class Importacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    @Column(name = "fecha_proceso", nullable = false)
    private LocalDateTime fechaProceso;

    @Column(name = "total_filas", nullable = false)
    private Integer totalFilas;

    @Column(nullable = false)
    private Integer aceptados;

    @Column(nullable = false)
    private Integer rechazados;

    @Column(nullable = false, length = 50)
    private String estado;

    public Importacion() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public LocalDateTime getFechaProceso() { return fechaProceso; }
    public void setFechaProceso(LocalDateTime fechaProceso) { this.fechaProceso = fechaProceso; }

    public Integer getTotalFilas() { return totalFilas; }
    public void setTotalFilas(Integer totalFilas) { this.totalFilas = totalFilas; }

    public Integer getAceptados() { return aceptados; }
    public void setAceptados(Integer aceptados) { this.aceptados = aceptados; }

    public Integer getRechazados() { return rechazados; }
    public void setRechazados(Integer rechazados) { this.rechazados = rechazados; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}