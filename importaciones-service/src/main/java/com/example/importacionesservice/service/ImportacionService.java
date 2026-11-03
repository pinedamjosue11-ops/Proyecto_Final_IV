package com.example.importacionesservice.service;

import com.example.importacionesservice.dto.*;
import com.example.importacionesservice.model.Importacion;
import com.example.importacionesservice.model.ImportacionError;
import com.example.importacionesservice.repository.ImportacionErrorRepository;
import com.example.importacionesservice.repository.ImportacionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImportacionService {

    private final ImportacionRepository importacionRepository;
    private final ImportacionErrorRepository importacionErrorRepository;
    private final RestTemplate restTemplate;

    @Value("${catalogo.service.url}")
    private String catalogoServiceUrl;

    public ImportacionService(ImportacionRepository importacionRepository,
                              ImportacionErrorRepository importacionErrorRepository,
                              RestTemplate restTemplate) {
        this.importacionRepository = importacionRepository;
        this.importacionErrorRepository = importacionErrorRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public ImportacionResumenDto procesarArchivoCsv(MultipartFile archivo) {
        Importacion importacion = new Importacion();
        importacion.setNombreArchivo(archivo.getOriginalFilename());
        importacion.setFechaProceso(LocalDateTime.now());
        importacion.setTotalFilas(0);
        importacion.setAceptados(0);
        importacion.setRechazados(0);
        importacion.setEstado("PROCESANDO");

        importacion = importacionRepository.save(importacion);

        List<ProductoItemDto> productosValidos = new ArrayList<>();
        List<ImportacionError> erroresLocales = new ArrayList<>();
        int numeroFila = 1;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(archivo.getInputStream()))) {
            String linea;
            boolean primeraLinea = true;

            while ((linea = br.readLine()) != null) {
                if (primeraLinea) {
                    primeraLinea = false;
                    continue; // Saltar cabecera
                }
                numeroFila++;

                String[] campos = linea.split(",");
                if (campos.length < 5) {
                    ImportacionError error = new ImportacionError();
                    error.setImportacion(importacion);
                    error.setNumeroFila(numeroFila);
                    error.setDatosRecibidos(linea);
                    error.setMotivo("Fila incompleta o formato CSV incorrecto");
                    erroresLocales.add(error);
                    continue;
                }

                try {
                    String codigo = campos[0].trim();
                    String nombre = campos[1].trim();
                    String categoria = campos[2].trim();
                    BigDecimal precio = new BigDecimal(campos[3].trim());
                    Integer existencia = Integer.parseInt(campos[4].trim());

                    productosValidos.add(new ProductoItemDto(
                            numeroFila, codigo, nombre, categoria, precio, existencia
                    ));
                } catch (Exception e) {
                    ImportacionError error = new ImportacionError();
                    error.setImportacion(importacion);
                    error.setNumeroFila(numeroFila);
                    error.setDatosRecibidos(linea);
                    error.setMotivo("Error en tipos de datos: " + e.getMessage());
                    erroresLocales.add(error);
                }
            }
        } catch (Exception e) {
            importacion.setEstado("FALLIDO");
            importacionRepository.save(importacion);
            throw new RuntimeException("Error leyendo el archivo CSV: " + e.getMessage());
        }

        int aceptados = 0;
        int rechazados = erroresLocales.size();

        // Comunicación REST con catalogo-service si hay productos parsed correctamente
        if (!productosValidos.isEmpty()) {
            try {
                LoteResultDto respuestaCatalogo = restTemplate.postForObject(
                        catalogoServiceUrl,
                        productosValidos,
                        LoteResultDto.class
                );

                if (respuestaCatalogo != null && respuestaCatalogo.resultados() != null) {
                    for (ProductoResultDto res : respuestaCatalogo.resultados()) {
                        if (res.exitoso()) {
                            aceptados++;
                        } else {
                            rechazados++;
                            ImportacionError err = new ImportacionError();
                            err.setImportacion(importacion);
                            err.setNumeroFila(res.numeroFila());
                            err.setDatosRecibidos("Codigo: " + res.codigo());
                            err.setMotivo("Catálogo: " + res.motivoError());
                            erroresLocales.add(err);
                        }
                    }
                }
            } catch (Exception e) {
                importacion.setEstado("ERROR_COMUNICACION_CATALOGO");
                importacionRepository.save(importacion);
                throw new RuntimeException("No se pudo conectar con catalogo-service: " + e.getMessage());
            }
        }

        if (!erroresLocales.isEmpty()) {
            importacionErrorRepository.saveAll(erroresLocales);
        }

        importacion.setTotalFilas(numeroFila - 1);
        importacion.setAceptados(aceptados);
        importacion.setRechazados(rechazados);
        importacion.setEstado(rechazados == 0 ? "COMPLETADO" : "COMPLETADO_CON_ERRORES");

        importacionRepository.save(importacion);

        return new ImportacionResumenDto(
                importacion.getId(),
                importacion.getNombreArchivo(),
                importacion.getFechaProceso(),
                importacion.getTotalFilas(),
                importacion.getAceptados(),
                importacion.getRechazados(),
                importacion.getEstado()
        );
    }
}