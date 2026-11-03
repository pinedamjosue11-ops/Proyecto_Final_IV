package com.example.importacionesservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "importacion_errores")
public class ImportacionError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "importacion_id", nullable = false)
    private Importacion importacion;

    @Column(name = "numero_fila", nullable = false)
    private Integer numeroFila;

    @Column(name = "datos_recibidos", columnDefinition = "TEXT")
    private String datosRecibidos;

    @Column(nullable = false)
    private String motivo;

    public ImportacionError() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Importacion getImportacion() { return importacion; }
    public void setImportacion(Importacion importacion) { this.importacion = importacion; }

    public Integer getNumeroFila() { return numeroFila; }
    public void setNumeroFila(Integer numeroFila) { this.numeroFila = numeroFila; }

    public String getDatosRecibidos() { return datosRecibidos; }
    public void setDatosRecibidos(String datosRecibidos) { this.datosRecibidos = datosRecibidos; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}