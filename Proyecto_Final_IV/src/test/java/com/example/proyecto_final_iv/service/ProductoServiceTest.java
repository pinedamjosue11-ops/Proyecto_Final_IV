package com.example.proyecto_final_iv.service;

import com.example.proyecto_final_iv.model.Producto;
import com.example.proyecto_final_iv.dto.ProductoRequestDto;
import com.example.proyecto_final_iv.model.Categoria;
import com.example.proyecto_final_iv.repository.ICategoriaRepository;
import com.example.proyecto_final_iv.repository.iProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private iProductoRepository productoRepository;

    @Mock
    private ICategoriaRepository categoriaRepository;

    @InjectMocks
    private ProductoService service;

    @Test
    void searchesProductsByTextAndCategoryWithPagination() {
        var pageable = PageRequest.of(1, 10);
        var productos = new PageImpl<>(List.of(new Producto()), pageable, 11);
        when(productoRepository.buscar("teclado", "Perifericos", pageable)).thenReturn(productos);

        var resultado = service.listarProductos("teclado", "Perifericos", pageable);

        assertEquals(11, resultado.getTotalElements());
        assertEquals(1, resultado.getNumber());
        verify(productoRepository).buscar("teclado", "Perifericos", pageable);
    }

    @Test
    void rejectsProductFieldsThatExceedDatabaseColumnLimits() {
        var producto = new ProductoRequestDto(
                2, "C".repeat(51), "Nombre", "Perifericos",
                java.math.BigDecimal.TEN, 1
        );

        var resultado = service.procesarLote(List.of(producto));

        assertFalse(resultado.resultados().get(0).exitoso());
        verifyNoInteractions(categoriaRepository);
        verifyNoInteractions(productoRepository);
    }

    @Test
    void rejectsDuplicateProductCodesAndExplainsTheReason() {
        when(productoRepository.existsByCodigo("EXISTS")).thenReturn(true);
        var producto = new ProductoRequestDto(
                2, "EXISTS", "Mouse", "Perifericos",
                java.math.BigDecimal.TEN, 1
        );

        var resultado = service.procesarLote(List.of(producto));

        assertFalse(resultado.resultados().get(0).exitoso());
        assertEquals("El código del producto ya existe", resultado.resultados().get(0).motivoError());
        verify(productoRepository, never()).save(org.mockito.ArgumentMatchers.any(Producto.class));
        verifyNoInteractions(categoriaRepository);
    }

    @Test
    void acceptsProductWhenItsFieldsAndCategoryAreValid() {
        var categoria = new Categoria(1L, "Perifericos");
        when(productoRepository.existsByCodigo("NEW-1")).thenReturn(false);
        when(categoriaRepository.findByNombreIgnoreCase("Perifericos")).thenReturn(java.util.Optional.of(categoria));
        when(productoRepository.save(org.mockito.ArgumentMatchers.any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var producto = new ProductoRequestDto(
                2, "NEW-1", "Mouse", "Perifericos",
                java.math.BigDecimal.TEN, 0
        );

        var resultado = service.procesarLote(List.of(producto));

        assertEquals(1, resultado.resultados().size());
        org.junit.jupiter.api.Assertions.assertTrue(resultado.resultados().get(0).exitoso());
        verify(productoRepository).save(org.mockito.ArgumentMatchers.any(Producto.class));
    }

    @Test
    void rejectsInvalidPriceAndNegativeInventory() {
        var precioInvalido = new ProductoRequestDto(
                2, "NEW-1", "Mouse", "Perifericos",
                java.math.BigDecimal.ZERO, 1
        );
        var existenciaInvalida = new ProductoRequestDto(
                3, "NEW-2", "Teclado", "Perifericos",
                java.math.BigDecimal.TEN, -1
        );

        var resultado = service.procesarLote(List.of(precioInvalido, existenciaInvalida));

        assertEquals(2, resultado.resultados().size());
        assertEquals("El precio debe ser mayor a 0", resultado.resultados().get(0).motivoError());
        assertEquals("La existencia debe ser mayor o igual a 0", resultado.resultados().get(1).motivoError());
        verifyNoInteractions(productoRepository, categoriaRepository);
    }
}
