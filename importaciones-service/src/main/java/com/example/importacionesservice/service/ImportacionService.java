package com.example.importacionesservice.service;

import com.example.importacionesservice.dto.*;
import com.example.importacionesservice.model.Importacion;
import com.example.importacionesservice.model.ImportacionError;
import com.example.importacionesservice.repository.ImportacionErrorRepository;
import com.example.importacionesservice.repository.ImportacionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ImportacionService {

    private static final Logger log = LoggerFactory.getLogger(ImportacionService.class);
    private static final List<String> ENCABEZADOS_ESPERADOS =
            List.of("codigo", "nombre", "categoria", "precio", "existencia");

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

    public Page<ImportacionResumenDto> listarImportaciones(Pageable pageable) {
        return importacionRepository.findAllByOrderByFechaProcesoDesc(pageable)
                .map(this::convertirResumen);
    }

    public Optional<ImportacionDetalleDto> obtenerImportacion(Long id) {
        return importacionRepository.findById(id).map(importacion -> new ImportacionDetalleDto(
                convertirResumen(importacion),
                convertirErrores(importacionErrorRepository
                        .findByImportacionIdOrderByNumeroFilaAsc(id))
        ));
    }

    public Optional<List<ImportacionErrorDto>> listarErrores(Long id) {
        return importacionRepository.findById(id).map(importacion ->
                convertirErrores(importacionErrorRepository
                        .findByImportacionIdOrderByNumeroFilaAsc(importacion.getId()))
        );
    }

    public ImportacionResumenDto procesarArchivoCsv(MultipartFile archivo) {
        Importacion importacion = new Importacion();
        importacion.setNombreArchivo(
                archivo.getOriginalFilename() == null ? "archivo.csv" : archivo.getOriginalFilename()
        );
        importacion.setFechaProceso(LocalDateTime.now());
        importacion.setTotalFilas(0);
        importacion.setAceptados(0);
        importacion.setRechazados(0);
        importacion.setEstado("PROCESANDO");

        importacion = importacionRepository.save(importacion);

        List<ProductoItemDto> productosValidos = new ArrayList<>();
        List<ImportacionError> erroresLocales = new ArrayList<>();
        int totalFilas = 0;
        int numeroFila = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {
            String encabezado = br.readLine();
            if (encabezado == null || encabezado.isBlank()) {
                agregarError(erroresLocales, importacion, 1, encabezado,
                        "El archivo CSV está vacío o no contiene encabezados");
                return finalizar(importacion, totalFilas, 0, erroresLocales, "FALLIDO_VALIDACION");
            }

            try {
                List<String> columnas = parsearLineaCsv(encabezado.replaceFirst("^\\uFEFF", ""));
                List<String> encabezados = columnas.stream()
                        .map(String::trim)
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .toList();
                if (!ENCABEZADOS_ESPERADOS.equals(encabezados)) {
                    agregarError(erroresLocales, importacion, 1, encabezado,
                            "Encabezados incorrectos. Se esperaba: codigo,nombre,categoria,precio,existencia");
                    return finalizar(importacion, totalFilas, 0, erroresLocales, "FALLIDO_VALIDACION");
                }
            } catch (IllegalArgumentException e) {
                agregarError(erroresLocales, importacion, 1, encabezado,
                        "El encabezado no tiene un formato CSV válido");
                return finalizar(importacion, totalFilas, 0, erroresLocales, "FALLIDO_VALIDACION");
            }

            String linea;
            while ((linea = br.readLine()) != null) {
                numeroFila++;
                totalFilas++;
                int filaArchivo = numeroFila + 1;

                List<String> campos;
                try {
                    campos = parsearLineaCsv(linea);
                } catch (IllegalArgumentException e) {
                    agregarError(erroresLocales, importacion, filaArchivo, linea,
                            "La fila no tiene un formato CSV válido");
                    continue;
                }

                if (campos.size() != ENCABEZADOS_ESPERADOS.size()) {
                    agregarError(erroresLocales, importacion, filaArchivo, linea,
                            "La fila debe contener exactamente cinco columnas");
                    continue;
                }

                try {
                    String codigo = campos.get(0).trim();
                    String nombre = campos.get(1).trim();
                    String categoria = campos.get(2).trim();
                    BigDecimal precio = new BigDecimal(campos.get(3).trim());
                    Integer existencia = Integer.valueOf(campos.get(4).trim());

                    if (codigo.isBlank() || nombre.isBlank() || categoria.isBlank()) {
                        throw new IllegalArgumentException("Código, nombre y categoría son obligatorios");
                    }
                    if (precio.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("El precio debe ser mayor que cero");
                    }
                    if (existencia < 0) {
                        throw new IllegalArgumentException("La existencia no puede ser negativa");
                    }
                    productosValidos.add(new ProductoItemDto(
                            filaArchivo, codigo, nombre, categoria, precio, existencia
                    ));
                } catch (NumberFormatException e) {
                    agregarError(erroresLocales, importacion, filaArchivo, linea,
                            "El precio debe ser numérico y la existencia debe ser un número entero");
                } catch (IllegalArgumentException e) {
                    agregarError(erroresLocales, importacion, filaArchivo, linea, e.getMessage());
                }
            }
            if (totalFilas == 0) {
                agregarError(erroresLocales, importacion, 0, encabezado,
                        "El archivo CSV no contiene filas de productos");
                return finalizar(importacion, totalFilas, 0, erroresLocales, "FALLIDO_VALIDACION");
            }
        } catch (Exception e) {
            log.error("No se pudo leer el archivo de la importación {}", importacion.getId(), e);
            agregarError(erroresLocales, importacion, 0, importacion.getNombreArchivo(),
                    "No se pudo leer el archivo CSV");
            return finalizar(importacion, totalFilas, 0, erroresLocales, "FALLIDO");
        }

        int aceptados = 0;
        int rechazados = erroresLocales.size();

        if (!productosValidos.isEmpty()) {
            try {
                LoteResultDto respuestaCatalogo = restTemplate.postForObject(
                        catalogoServiceUrl,
                        productosValidos,
                        LoteResultDto.class
                );

                if (respuestaCatalogo == null || respuestaCatalogo.resultados() == null) {
                    throw new IllegalStateException("El catálogo devolvió una respuesta vacía");
                }
                for (ProductoResultDto res : respuestaCatalogo.resultados()) {
                    if (res.exitoso()) {
                        aceptados++;
                    } else {
                        rechazados++;
                        agregarError(erroresLocales, importacion, res.numeroFila(),
                                "Código: " + res.codigo(), "Catálogo: " + res.motivoError());
                    }
                }
                if (respuestaCatalogo.resultados().size() < productosValidos.size()) {
                    int faltantes = productosValidos.size() - respuestaCatalogo.resultados().size();
                    rechazados += faltantes;
                    agregarError(erroresLocales, importacion, 0, importacion.getNombreArchivo(),
                            "El catálogo no devolvió resultado para " + faltantes + " fila(s)");
                }
            } catch (Exception e) {
                log.error("Error comunicándose con el catálogo para la importación {}",
                        importacion.getId(), e);
                importacion.setEstado("ERROR_COMUNICACION_CATALOGO");
                importacion.setTotalFilas(totalFilas);
                agregarError(erroresLocales, importacion, 0, importacion.getNombreArchivo(),
                        "No se pudo comunicar con el catálogo de productos");
                importacion.setRechazados(totalFilas);
                importacionRepository.save(importacion);
                importacionErrorRepository.saveAll(erroresLocales);
                return convertirResumen(importacion);
            }
        }

        String estado = rechazados == 0 ? "COMPLETADO" : "COMPLETADO_CON_ERRORES";
        return finalizar(importacion, totalFilas, aceptados, erroresLocales, estado, rechazados);
    }

    private ImportacionResumenDto finalizar(
            Importacion importacion,
            int totalFilas,
            int aceptados,
            List<ImportacionError> errores,
            String estado) {
        return finalizar(importacion, totalFilas, aceptados, errores, estado, errores.size());
    }

    private ImportacionResumenDto finalizar(
            Importacion importacion,
            int totalFilas,
            int aceptados,
            List<ImportacionError> errores,
            String estado,
            int rechazados) {
        importacion.setTotalFilas(totalFilas);
        importacion.setAceptados(aceptados);
        importacion.setRechazados(rechazados);
        importacion.setEstado(estado);
        importacionRepository.save(importacion);
        if (!errores.isEmpty()) {
            importacionErrorRepository.saveAll(errores);
            log.warn("Importación {} terminó con {} fila(s) para revisión",
                    importacion.getId(), rechazados);
        } else {
            log.info("Importación {} terminó sin filas rechazadas", importacion.getId());
        }
        log.info("Importación {} finalizada: estado={}, filas={}, aceptadas={}, rechazadas={}",
                importacion.getId(), estado, totalFilas, aceptados, rechazados);
        return convertirResumen(importacion);
    }

    private void agregarError(
            List<ImportacionError> errores,
            Importacion importacion,
            int numeroFila,
            String datos,
            String motivo) {
        ImportacionError error = new ImportacionError();
        error.setImportacion(importacion);
        error.setNumeroFila(numeroFila);
        error.setDatosRecibidos(datos == null || datos.length() <= 2000
                ? datos
                : datos.substring(0, 2000));
        error.setMotivo(motivo);
        errores.add(error);
    }

    private List<String> parsearLineaCsv(String linea) {
        List<String> campos = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreComillas = false;
        boolean comillasCerradas = false;

        for (int i = 0; i < linea.length(); i++) {
            char actual = linea.charAt(i);
            if (entreComillas) {
                if (actual == '"' && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    campo.append('"');
                    i++;
                } else if (actual == '"') {
                    entreComillas = false;
                    comillasCerradas = true;
                } else {
                    campo.append(actual);
                }
            } else if (actual == ',') {
                campos.add(campo.toString());
                campo.setLength(0);
                comillasCerradas = false;
            } else if (actual == '"') {
                if (campo.isEmpty() && !comillasCerradas) {
                    entreComillas = true;
                } else {
                    throw new IllegalArgumentException("Comilla inesperada");
                }
            } else {
                if (comillasCerradas) {
                    throw new IllegalArgumentException("Contenido inesperado después de un campo entre comillas");
                }
                campo.append(actual);
            }
        }
        if (entreComillas) {
            throw new IllegalArgumentException("Comillas sin cerrar");
        }
        campos.add(campo.toString());
        return campos;
    }

    private ImportacionResumenDto convertirResumen(Importacion importacion) {
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

    private List<ImportacionErrorDto> convertirErrores(List<ImportacionError> errores) {
        return errores.stream()
                .map(error -> new ImportacionErrorDto(
                        error.getNumeroFila(),
                        error.getDatosRecibidos(),
                        error.getMotivo()
                ))
                .toList();
    }
}