package com.example.product.service;

import com.example.lib.exception.InsufficientStockException;
import com.example.product.entity.Product;
import com.example.product.exception.ProductNotFoundException;
import com.example.product.repository.ProductRepository;
import com.example.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private static Product product(Long id, String category, String brand, String price, int stock, double rating) {
        return Product.builder()
                .id(id)
                .name("Product " + id)
                .description("Desc")
                .price(new BigDecimal(price))
                .category(category)
                .brand(brand)
                .stock(stock)
                .rating(rating)
                .reviewCount(10)
                .build();
    }

    private static Product simple(Long id, int stock) {
        return product(id, "electronics", "Brand", "99.99", stock, 4.0);
    }

    // ─────────────────────────── getProducts ─────────────────────────────

    @Nested
    @DisplayName("getProducts")
    class GetProducts {

        @Test
        @DisplayName("ids supplied → batch fetch via findAllById, no pagination")
        void byIds() {
            when(productRepository.findAllById(List.of(1L, 2L)))
                    .thenReturn(List.of(simple(1L, 5), simple(2L, 3)));

            var page = productService.getProducts(null, List.of(1L, 2L), null, null, null, null, null, null, "relevance", 0, 20);

            assertThat(page.getContent()).hasSize(2);
            verify(productRepository).findAllById(List.of(1L, 2L));
            verify(productRepository, never()).findAll(any(Specification.class), any(PageRequest.class));
        }

        @Test
        @DisplayName("empty ids list falls back to specification path")
        void emptyIdsFallsThrough() {
            when(productRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of(simple(1L, 1))));

            productService.getProducts(null, List.of(), null, null, null, null, null, null, "relevance", 0, 20);

            verify(productRepository, never()).findAllById(any());
            verify(productRepository).findAll(any(Specification.class), any(PageRequest.class));
        }

        @Test
        @DisplayName("default sort 'relevance' → ASC by id")
        void relevanceSort() {
            var captor = ArgumentCaptor.forClass(PageRequest.class);
            when(productRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(new PageImpl<>(List.of()));

            productService.getProducts(null, null, null, null, null, null, null, null, "relevance", 0, 20);

            assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
        }

        @Test
        @DisplayName("sort 'price-asc' → ASC by price")
        void priceAscSort() {
            var captor = ArgumentCaptor.forClass(PageRequest.class);
            when(productRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(new PageImpl<>(List.of()));

            productService.getProducts(null, null, null, null, null, null, null, null, "price-asc", 0, 20);

            assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "price"));
        }

        @Test
        @DisplayName("sort 'price-desc' → DESC by price")
        void priceDescSort() {
            var captor = ArgumentCaptor.forClass(PageRequest.class);
            when(productRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(new PageImpl<>(List.of()));

            productService.getProducts(null, null, null, null, null, null, null, null, "price-desc", 0, 20);

            assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "price"));
        }

        @Test
        @DisplayName("sort 'rating-desc' → DESC rating + DESC reviewCount")
        void ratingDescSort() {
            var captor = ArgumentCaptor.forClass(PageRequest.class);
            when(productRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(new PageImpl<>(List.of()));

            productService.getProducts(null, null, null, null, null, null, null, null, "rating-desc", 0, 20);

            assertThat(captor.getValue().getSort()).isEqualTo(
                    Sort.by(Sort.Direction.DESC, "rating").and(Sort.by(Sort.Direction.DESC, "reviewCount")));
        }

        @Test
        @DisplayName("sort 'newest' → DESC by id")
        void newestSort() {
            var captor = ArgumentCaptor.forClass(PageRequest.class);
            when(productRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(new PageImpl<>(List.of()));

            productService.getProducts(null, null, null, null, null, null, null, null, "newest", 0, 20);

            assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "id"));
        }

        @Test
        @DisplayName("unknown sort falls back to default ASC by id")
        void unknownSortFallback() {
            var captor = ArgumentCaptor.forClass(PageRequest.class);
            when(productRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(new PageImpl<>(List.of()));

            productService.getProducts(null, null, null, null, null, null, null, null, "garbage-sort", 0, 20);

            assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
        }

        @Test
        @DisplayName("null sort treated as 'relevance'")
        void nullSort() {
            var captor = ArgumentCaptor.forClass(PageRequest.class);
            when(productRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(new PageImpl<>(List.of()));

            productService.getProducts(null, null, null, null, null, null, null, null, null, 0, 20);

            assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
        }

        @Test
        @DisplayName("page + limit forwarded to PageRequest")
        void pageAndLimit() {
            var captor = ArgumentCaptor.forClass(PageRequest.class);
            when(productRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(new PageImpl<>(List.of()));

            productService.getProducts(null, null, null, null, null, null, null, null, "relevance", 3, 25);

            assertThat(captor.getValue().getPageNumber()).isEqualTo(3);
            assertThat(captor.getValue().getPageSize()).isEqualTo(25);
        }

        @Test
        @DisplayName("ProductResponse maps entity fields correctly")
        void mapping() {
            var p = product(1L, "Jewelry", "Acme", "12.34", 7, 4.7);
            when(productRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of(p)));

            var page = productService.getProducts(null, null, null, null, null, null, null, null, "relevance", 0, 20);

            var dto = page.getContent().getFirst();
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("Product 1");
            assertThat(dto.category()).isEqualTo("Jewelry");
            assertThat(dto.brand()).isEqualTo("Acme");
            assertThat(dto.price()).isEqualByComparingTo("12.34");
            assertThat(dto.stock()).isEqualTo(7);
            assertThat(dto.rating()).isEqualTo(4.7);
        }
    }

    // ─────────────────────────── getProductById ─────────────────────────

    @Nested
    @DisplayName("getProductById")
    class GetProductById {

        @Test
        @DisplayName("found → maps to response")
        void found() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(simple(1L, 5)));

            var result = productService.getProductById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.stock()).isEqualTo(5);
        }

        @Test
        @DisplayName("not found → ProductNotFoundException with id in message")
        void notFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductById(99L))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ─────────────────────────── getProductFilters ──────────────────────

    @Nested
    @DisplayName("getProductFilters")
    class Filters {

        @Test
        @DisplayName("returns categories + brands from repository")
        void returnsBoth() {
            when(productRepository.findDistinctCategories()).thenReturn(List.of("electronics", "jewelry"));
            when(productRepository.findDistinctBrands()).thenReturn(List.of("A", "B"));

            var resp = productService.getProductFilters();

            assertThat(resp.categories()).containsExactly("electronics", "jewelry");
            assertThat(resp.brands()).containsExactly("A", "B");
        }

        @Test
        @DisplayName("empty results pass through")
        void empty() {
            when(productRepository.findDistinctCategories()).thenReturn(List.of());
            when(productRepository.findDistinctBrands()).thenReturn(List.of());

            var resp = productService.getProductFilters();

            assertThat(resp.categories()).isEmpty();
            assertThat(resp.brands()).isEmpty();
        }
    }

    // ─────────────────────────── deductStock ────────────────────────────

    @Nested
    @DisplayName("deductStock")
    class DeductStock {

        @Test
        @DisplayName("happy path → reduces stock + saves")
        void reduces() {
            var p = simple(1L, 10);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));

            productService.deductStock(1L, 3);

            assertThat(p.getStock()).isEqualTo(7);
            verify(productRepository).save(p);
        }

        @Test
        @DisplayName("quantity == stock → succeeds, stock=0")
        void exactBoundary() {
            var p = simple(1L, 5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));

            productService.deductStock(1L, 5);

            assertThat(p.getStock()).isZero();
            verify(productRepository).save(p);
        }

        @Test
        @DisplayName("quantity > stock by 1 → InsufficientStockException, no save")
        void overByOne() {
            var p = simple(1L, 5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> productService.deductStock(1L, 6))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("5 in stock")
                    .hasMessageContaining("requested 6");

            verify(productRepository, never()).save(any());
            assertThat(p.getStock()).isEqualTo(5);
        }

        @Test
        @DisplayName("zero stock product → throws on any deduction")
        void zeroStock() {
            var p = simple(1L, 0);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> productService.deductStock(1L, 1))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        @DisplayName("missing product → ProductNotFoundException")
        void notFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deductStock(99L, 1))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    // ─────────────────────────── restoreStock ───────────────────────────

    @Nested
    @DisplayName("restoreStock")
    class RestoreStock {

        @Test
        @DisplayName("adds quantity")
        void adds() {
            var p = simple(1L, 3);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));

            productService.restoreStock(1L, 4);

            assertThat(p.getStock()).isEqualTo(7);
            verify(productRepository).save(p);
        }

        @Test
        @DisplayName("missing product → ProductNotFoundException")
        void notFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.restoreStock(99L, 1))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    // ─────────────────────────── setStock ───────────────────────────────

    @Nested
    @DisplayName("setStock")
    class SetStock {

        @Test
        @DisplayName("sets stock to provided value")
        void sets() {
            var p = simple(1L, 99);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));
            when(productRepository.save(p)).thenReturn(p);

            var resp = productService.setStock(1L, 5);

            assertThat(p.getStock()).isEqualTo(5);
            assertThat(resp.stock()).isEqualTo(5);
        }

        @Test
        @DisplayName("negative value clamped to 0")
        void clampsNegative() {
            var p = simple(1L, 99);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));
            when(productRepository.save(p)).thenReturn(p);

            productService.setStock(1L, -10);

            assertThat(p.getStock()).isZero();
        }
    }

    // ─────────────────────────── updateProduct ──────────────────────────

    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("updates all fields, trims whitespace")
        void updatesAll() {
            var p = simple(1L, 5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));
            when(productRepository.save(p)).thenReturn(p);

            productService.updateProduct(1L, "  New Name  ", "  desc  ",
                    new BigDecimal("12.50"), "  Jewelry  ", "  AcmeCo  ", 9);

            assertThat(p.getName()).isEqualTo("New Name");
            assertThat(p.getDescription()).isEqualTo("desc");
            assertThat(p.getPrice()).isEqualByComparingTo("12.50");
            assertThat(p.getCategory()).isEqualTo("Jewelry");
            assertThat(p.getBrand()).isEqualTo("AcmeCo");
            assertThat(p.getStock()).isEqualTo(9);
        }

        @Test
        @DisplayName("blank name → 400")
        void blankName() {
            var p = simple(1L, 5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> productService.updateProduct(1L, "  ", "d",
                    new BigDecimal("1"), "cat", "b", 0))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("name");
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("blank category → 400")
        void blankCategory() {
            var p = simple(1L, 5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> productService.updateProduct(1L, "n", "d",
                    new BigDecimal("1"), "  ", "b", 0))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("category");
        }

        @Test
        @DisplayName("null price → 400")
        void nullPrice() {
            var p = simple(1L, 5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> productService.updateProduct(1L, "n", "d",
                    null, "c", "b", 0))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("price");
        }

        @Test
        @DisplayName("negative price → 400")
        void negativePrice() {
            var p = simple(1L, 5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> productService.updateProduct(1L, "n", "d",
                    new BigDecimal("-1"), "c", "b", 0))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("non-negative");
        }

        @Test
        @DisplayName("zero price allowed (free)")
        void zeroPriceAllowed() {
            var p = simple(1L, 5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));
            when(productRepository.save(p)).thenReturn(p);

            productService.updateProduct(1L, "n", "d",
                    BigDecimal.ZERO, "c", "b", 1);

            assertThat(p.getPrice()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("null brand defaults to empty string")
        void nullBrand() {
            var p = simple(1L, 5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));
            when(productRepository.save(p)).thenReturn(p);

            productService.updateProduct(1L, "n", "d",
                    new BigDecimal("1"), "c", null, 1);

            assertThat(p.getBrand()).isEmpty();
        }

        @Test
        @DisplayName("null description defaults to empty string")
        void nullDescription() {
            var p = simple(1L, 5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));
            when(productRepository.save(p)).thenReturn(p);

            productService.updateProduct(1L, "n", null,
                    new BigDecimal("1"), "c", "b", 1);

            assertThat(p.getDescription()).isEmpty();
        }

        @Test
        @DisplayName("negative stock clamped to 0")
        void negativeStockClamped() {
            var p = simple(1L, 5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));
            when(productRepository.save(p)).thenReturn(p);

            productService.updateProduct(1L, "n", "d",
                    new BigDecimal("1"), "c", "b", -7);

            assertThat(p.getStock()).isZero();
        }

        @Test
        @DisplayName("not found → ProductNotFoundException")
        void notFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(99L, "n", "d",
                    new BigDecimal("1"), "c", "b", 1))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    // ─────────────────────────── addImage ───────────────────────────────

    @Nested
    @DisplayName("addImage")
    class AddImage {

        @Test
        @DisplayName("null image → 400")
        void nullImage() {
            var p = simple(1L, 5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> productService.addImage(1L, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("empty image → 400")
        void emptyImage() {
            var p = simple(1L, 5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));
            var img = new org.springframework.mock.web.MockMultipartFile(
                    "image", "x.png", "image/png", new byte[0]);

            assertThatThrownBy(() -> productService.addImage(1L, img))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("unsupported MIME → 400")
        void unsupportedType() {
            var p = simple(1L, 5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));
            var img = new org.springframework.mock.web.MockMultipartFile(
                    "image", "x.gif", "image/gif", new byte[]{1, 2, 3});

            assertThatThrownBy(() -> productService.addImage(1L, img))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("JPEG, PNG, and WebP");
        }

        @Test
        @DisplayName("missing product → ProductNotFoundException")
        void notFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());
            var img = new org.springframework.mock.web.MockMultipartFile(
                    "image", "x.png", "image/png", new byte[]{1});

            assertThatThrownBy(() -> productService.addImage(99L, img))
                    .isInstanceOf(ProductNotFoundException.class);
        }

        @Test
        @DisplayName("valid PNG → adds filename to images, saves, returns response (uses temp dir)")
        void valid(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) {
            var p = simple(1L, 5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));
            when(productRepository.save(p)).thenReturn(p);

            org.springframework.test.util.ReflectionTestUtils.setField(
                    productService, "imagesPath", tempDir.toString());

            var img = new org.springframework.mock.web.MockMultipartFile(
                    "image", "x.png", "image/png", new byte[]{1, 2, 3, 4});

            var resp = productService.addImage(1L, img);

            assertThat(p.getImages()).hasSize(1);
            assertThat(p.getImages().get(0)).endsWith(".png");
            assertThat(resp.images()).hasSize(1);
        }
    }
}
