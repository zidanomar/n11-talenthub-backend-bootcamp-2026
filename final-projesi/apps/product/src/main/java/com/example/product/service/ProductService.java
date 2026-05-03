package com.example.product.service;

import com.example.product.dto.ProductFiltersResponse;
import com.example.product.dto.ProductResponse;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    Page<ProductResponse> getProducts(
            String category,
            List<Long> ids,
            String query,
            List<String> brands,
            Double minRating,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            String sort,
            int page,
            int limit
    );

    ProductResponse getProductById(Long id);

    ProductResponse createProduct(String name, String description, BigDecimal price, String category, String brand, int stock);

    ProductFiltersResponse getProductFilters();

    void deductStock(Long id, int quantity);

    void restoreStock(Long id, int quantity);

    ProductResponse setStock(Long id, int stock);

    ProductResponse updateProduct(Long id, String name, String description, BigDecimal price, String category, String brand, int stock);

    ProductResponse addImage(Long id, MultipartFile image);

    ProductResponse removeImage(Long id, String filename);
}
