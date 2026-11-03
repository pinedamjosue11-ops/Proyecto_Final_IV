package com.example.importacionesservice.service;

import com.example.importacionesservice.dto.LoteResultDto;
import com.example.importacionesservice.dto.ProductoResultDto;
import com.example.importacionesservice.model.Importacion;
import com.example.importacionesservice.model.ImportacionError;
import com.example.importacionesservice.repository.ImportacionErrorRepository;
import com.example.importacionesservice.repository.ImportacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportacionServiceTest {

    @Mock
    private ImportacionRepository importacionRepository;

    @Mock
    private ImportacionErrorRepository importacionErrorRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ImportacionService service;

    @Test
    void acceptsQuotedFieldsAndSendsProductsToCatalog() {
        prepararImportacion();
        when(restTemplate.postForObject(anyString(), any(), eq(LoteResultDto.class)))
                .thenReturn(new LoteResultDto(List.of(
                        new ProductoResultDto(2, "P100", true, null)
                )));
        MockMultipartFile archivo = csv(
                "codigo,nombre,categoria,precio,existencia\nP100,\"Teclado, mini\",Perifericos,100.00,2\n"
        );

        var resumen = service.procesarArchivoCsv(archivo);

        assertEquals(1, resumen.totalFilas());
        assertEquals(1, resumen.aceptados());
        assertEquals(0, resumen.rechazados());
        assertEquals("COMPLETADO", resumen.estado());
        verify(restTemplate).postForObject(anyString(), any(), eq(LoteResultDto.class));
    }

    @Test
    void recordsInvalidHeaderAndDoesNotCallCatalog() {
        prepararImportacion();
        MockMultipartFile archivo = csv("codigo,nombre,precio\nP100,Teclado,10\n");

        var resumen = service.procesarArchivoCsv(archivo);

        assertEquals("FALLIDO_VALIDACION", resumen.estado());
        assertEquals(1, resumen.rechazados());
        ArgumentCaptor<List<ImportacionError>> errores = ArgumentCaptor.forClass(List.class);
        verify(importacionErrorRepository).saveAll(errores.capture());
        assertTrue(errores.getValue().get(0).getMotivo().contains("Encabezados incorrectos"));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void recordsEmptyFileAsFailedValidation() {
        prepararImportacion();
        MockMultipartFile archivo = csv("");

        var resumen = service.procesarArchivoCsv(archivo);

        assertEquals(0, resumen.totalFilas());
        assertEquals(1, resumen.rechazados());
        assertEquals("FALLIDO_VALIDACION", resumen.estado());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void rejectsHeaderOnlyFileWithNoProductRows() {
        prepararImportacion();
        MockMultipartFile archivo = csv("codigo,nombre,categoria,precio,existencia\n");

        var resumen = service.procesarArchivoCsv(archivo);

        assertEquals(0, resumen.totalFilas());
        assertEquals(1, resumen.rechazados());
        assertEquals("FALLIDO_VALIDACION", resumen.estado());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void recordsInvalidRowsAndContinuesProcessingValidRows() {
        prepararImportacion();
        when(restTemplate.postForObject(anyString(), any(), eq(LoteResultDto.class)))
                .thenReturn(new LoteResultDto(List.of(
                        new ProductoResultDto(2, "P100", true, null)
                )));
        MockMultipartFile archivo = csv(
                "codigo,nombre,categoria,precio,existencia\n" +
                "P100,Teclado,Perifericos,100,2\n" +
                "P200,Mouse,Perifericos,no-es-numero,1\n"
        );

        var resumen = service.procesarArchivoCsv(archivo);

        assertEquals(2, resumen.totalFilas());
        assertEquals(1, resumen.aceptados());
        assertEquals(1, resumen.rechazados());
        assertEquals("COMPLETADO_CON_ERRORES", resumen.estado());
    }

    @Test
    void preservesImportAndErrorWhenCatalogIsUnavailable() {
        prepararImportacion();
        when(restTemplate.postForObject(anyString(), any(), eq(LoteResultDto.class)))
                .thenThrow(new IllegalStateException("No servers available"));
        MockMultipartFile archivo = csv(
                "codigo,nombre,categoria,precio,existencia\nP100,Teclado,Perifericos,100,2\n"
        );

        var resumen = service.procesarArchivoCsv(archivo);

        assertEquals("ERROR_COMUNICACION_CATALOGO", resumen.estado());
        verify(importacionRepository, org.mockito.Mockito.atLeast(2)).save(any(Importacion.class));
        ArgumentCaptor<List<ImportacionError>> errores = ArgumentCaptor.forClass(List.class);
        verify(importacionErrorRepository).saveAll(errores.capture());
        assertTrue(errores.getValue().stream()
                .anyMatch(error -> error.getMotivo().contains("No se pudo comunicar")));
    }

    private void prepararImportacion() {
        ReflectionTestUtils.setField(service, "catalogoServiceUrl", "http://catalogo-service/api/v1/productos/lote");
        when(importacionRepository.save(any(Importacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private MockMultipartFile csv(String contenido) {
        return new MockMultipartFile(
                "archivo",
                "productos.csv",
                "text/csv",
                contenido.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }
}
