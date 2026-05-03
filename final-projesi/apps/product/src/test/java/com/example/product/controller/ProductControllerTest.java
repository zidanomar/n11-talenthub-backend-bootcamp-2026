package com.example.product.controller;

import com.example.lib.exception.InsufficientStockException;
import com.example.product.dto.ProductFiltersResponse;
import com.example.product.dto.ProductResponse;
import com.example.product.exception.ProductNotFoundException;
import com.example.product.exception.RestExceptionHandler;
import com.example.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(RestExceptionHandler.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@DisplayName("ProductController")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private static ProductResponse sampleProduct(Long id) {
        return new ProductResponse(id, "Product " + id, "Desc",
                new BigDecimal("99.99"), "electronics", "Brand",
                4.5, 100, List.of(), 10);
    }

    // ─────────────────────────── GET /api/products ──────────────────────

    @Nested
    @DisplayName("GET /api/products")
    class GetProducts {

        @Test
        @DisplayName("defaults: relevance, page=0, limit=20")
        void defaults() throws Exception {
            when(productService.getProducts(isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), eq("relevance"), eq(0), eq(20)))
                    .thenReturn(new PageImpl<>(List.of(sampleProduct(1L))));

            mockMvc.perform(get("/api/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1));
        }

        @Test
        @DisplayName("category param forwarded")
        void category() throws Exception {
            when(productService.getProducts(eq("electronics"), isNull(), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), eq("relevance"), eq(0), eq(20)))
                    .thenReturn(new PageImpl<>(List.of(sampleProduct(1L))));

            mockMvc.perform(get("/api/products").param("category", "electronics"))
                    .andExpect(status().isOk());

            verify(productService).getProducts("electronics", null, null, null, null, null, null, null,
                    "relevance", 0, 20);
        }

        @Test
        @DisplayName("full filter set forwarded")
        void allFilters() throws Exception {
            when(productService.getProducts(
                    eq("Jewelry"), isNull(), eq("ring"), eq(List.of("Brand")),
                    eq(4.0), eq(new BigDecimal("100")), eq(new BigDecimal("500")),
                    isNull(), eq("price-desc"), eq(1), eq(12)))
                    .thenReturn(new PageImpl<>(List.of(sampleProduct(1L))));

            mockMvc.perform(get("/api/products")
                            .param("category", "Jewelry")
                            .param("q", "ring")
                            .param("brands", "Brand")
                            .param("minRating", "4.0")
                            .param("minPrice", "100")
                            .param("maxPrice", "500")
                            .param("sort", "price-desc")
                            .param("page", "1")
                            .param("limit", "12"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ids batch param forwarded")
        void byIds() throws Exception {
            when(productService.getProducts(isNull(), eq(List.of(1L, 2L)), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), eq("relevance"), eq(0), eq(20)))
                    .thenReturn(new PageImpl<>(List.of(sampleProduct(1L), sampleProduct(2L))));

            mockMvc.perform(get("/api/products").param("ids", "1,2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }

        @Test
        @DisplayName("invalid id format → 400")
        void invalidId() throws Exception {
            mockMvc.perform(get("/api/products").param("ids", "abc"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("invalid minPrice → 400")
        void invalidMinPrice() throws Exception {
            mockMvc.perform(get("/api/products").param("minPrice", "not-a-number"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("multiple brands forwarded as list")
        void multipleBrands() throws Exception {
            when(productService.getProducts(isNull(), isNull(), isNull(),
                    eq(List.of("A", "B", "C")), isNull(), isNull(), isNull(), isNull(),
                    eq("relevance"), eq(0), eq(20)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/products").param("brands", "A", "B", "C"))
                    .andExpect(status().isOk());
        }
    }

    // ─────────────────────────── GET /api/products/filters ──────────────

    @Nested
    @DisplayName("GET /api/products/filters")
    class Filters {

        @Test
        @DisplayName("returns categories + brands")
        void ok() throws Exception {
            when(productService.getProductFilters())
                    .thenReturn(new ProductFiltersResponse(
                            List.of("electronics", "jewelry"),
                            List.of("A", "B")));

            mockMvc.perform(get("/api/products/filters"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categories.length()").value(2))
                    .andExpect(jsonPath("$.brands[1]").value("B"));
        }
    }

    // ─────────────────────────── GET /api/products/{id} ─────────────────

    @Nested
    @DisplayName("GET /api/products/{id}")
    class GetById {

        @Test
        @DisplayName("found → 200 with full body")
        void found() throws Exception {
            when(productService.getProductById(1L)).thenReturn(sampleProduct(1L));

            mockMvc.perform(get("/api/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Product 1"))
                    .andExpect(jsonPath("$.stock").value(10));
        }

        @Test
        @DisplayName("not found → 404 with message")
        void notFound() throws Exception {
            when(productService.getProductById(99L)).thenThrow(new ProductNotFoundException(99L));

            mockMvc.perform(get("/api/products/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Product not found: 99"));
        }

        @Test
        @DisplayName("non-numeric id → 400")
        void badId() throws Exception {
            mockMvc.perform(get("/api/products/abc"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────── PATCH .../stock ────────────────────────

    @Nested
    @DisplayName("PATCH /api/products/{id}/stock (deduct)")
    class Deduct {

        @Test
        @DisplayName("204 No Content")
        void ok() throws Exception {
            doNothing().when(productService).deductStock(1L, 3);

            mockMvc.perform(patch("/api/products/1/stock")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quantity\":3}"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("insufficient stock → 422")
        void insufficient() throws Exception {
            doThrow(new InsufficientStockException(1L, 5, 2))
                    .when(productService).deductStock(1L, 5);

            mockMvc.perform(patch("/api/products/1/stock")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quantity\":5}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value(
                            "Product 1 only has 2 in stock, requested 5"));
        }

        @Test
        @DisplayName("not found → 404")
        void notFound() throws Exception {
            doThrow(new ProductNotFoundException(99L))
                    .when(productService).deductStock(99L, 1);

            mockMvc.perform(patch("/api/products/99/stock")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quantity\":1}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("malformed body → 400")
        void malformed() throws Exception {
            mockMvc.perform(patch("/api/products/1/stock")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("not-json"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /api/products/{id}/stock/restore")
    class Restore {

        @Test
        @DisplayName("204 No Content")
        void ok() throws Exception {
            doNothing().when(productService).restoreStock(1L, 2);

            mockMvc.perform(patch("/api/products/1/stock/restore")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quantity\":2}"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("not found → 404")
        void notFound() throws Exception {
            doThrow(new ProductNotFoundException(99L))
                    .when(productService).restoreStock(99L, 1);

            mockMvc.perform(patch("/api/products/99/stock/restore")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quantity\":1}"))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────── merchant routes ─────────────────────────

    @Nested
    @DisplayName("PATCH /api/products/{id}/stock/set (merchant)")
    class SetStockMerchant {

        @Test
        @DisplayName("MERCHANT role → 200")
        void ok() throws Exception {
            when(productService.setStock(1L, 50)).thenReturn(sampleProduct(1L));

            mockMvc.perform(patch("/api/products/1/stock/set")
                            .header("X-User-Roles", "MERCHANT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"stock\":50}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("USER role → 403")
        void forbidden() throws Exception {
            mockMvc.perform(patch("/api/products/1/stock/set")
                            .header("X-User-Roles", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"stock\":50}"))
                    .andExpect(status().isForbidden());
            verify(productService, never()).setStock(any(), org.mockito.ArgumentMatchers.anyInt());
        }

        @Test
        @DisplayName("missing role header → 403")
        void noRole() throws Exception {
            mockMvc.perform(patch("/api/products/1/stock/set")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"stock\":50}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("multi-role list including MERCHANT → 200")
        void multiRole() throws Exception {
            when(productService.setStock(1L, 50)).thenReturn(sampleProduct(1L));

            mockMvc.perform(patch("/api/products/1/stock/set")
                            .header("X-User-Roles", "USER, MERCHANT, OPS")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"stock\":50}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PATCH /api/products/{id} (merchant update)")
    class UpdateMerchant {

        @Test
        @DisplayName("MERCHANT → 200")
        void ok() throws Exception {
            when(productService.updateProduct(eq(1L), eq("New"), eq("d"),
                    eq(new BigDecimal("12.50")), eq("c"), eq("b"), eq(5)))
                    .thenReturn(sampleProduct(1L));

            mockMvc.perform(patch("/api/products/1")
                            .header("X-User-Roles", "MERCHANT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"New\",\"description\":\"d\",\"price\":12.50,\"category\":\"c\",\"brand\":\"b\",\"stock\":5}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("non-merchant → 403")
        void forbidden() throws Exception {
            mockMvc.perform(patch("/api/products/1")
                            .header("X-User-Roles", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"x\",\"description\":\"\",\"price\":1,\"category\":\"c\",\"brand\":\"\",\"stock\":0}"))
                    .andExpect(status().isForbidden());
        }
    }
}
