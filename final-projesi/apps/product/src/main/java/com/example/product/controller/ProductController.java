package com.example.product.controller;

import com.example.product.dto.ProductFiltersResponse;
import com.example.product.dto.ProductResponse;
import com.example.product.service.ProductService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Value("${product.images.path:/app/images}")
    private String imagesPath;

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(required = false) List<String> brands,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(
                productService.getProducts(category, ids, query, brands, minRating, minPrice, maxPrice, inStock, sort, page, limit)
        );
    }

    @GetMapping("/filters")
    public ResponseEntity<ProductFiltersResponse> getProductFilters() {
        return ResponseEntity.ok(productService.getProductFilters());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) throws Exception {
        if (filename.contains("..") || filename.contains("/")) {
            return ResponseEntity.badRequest().build();
        }
        Path imagePath = Paths.get(imagesPath).resolve(filename).normalize();
        Resource resource = new FileSystemResource(imagePath);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String contentType = Files.probeContentType(imagePath);
        if (contentType == null) contentType = "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Void> deductStock(@PathVariable Long id,
                                            @RequestBody DeductStockRequest request) {
        productService.deductStock(id, request.quantity());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/stock/restore")
    public ResponseEntity<Void> restoreStock(@PathVariable Long id,
                                             @RequestBody RestoreStockRequest request) {
        productService.restoreStock(id, request.quantity());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/stock/set")
    public ResponseEntity<ProductResponse> setStock(@RequestHeader(value = "X-User-Roles", required = false) String roles,
                                                    @PathVariable Long id,
                                                    @RequestBody SetStockRequest request) {
        requireMerchant(roles);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock is required");
        }

        return ResponseEntity.ok(productService.setStock(id, request.stock()));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@RequestHeader(value = "X-User-Roles", required = false) String roles,
                                                         @RequestBody UpdateProductRequest request) {
        requireMerchant(roles);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product details are required");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(
                request.name(),
                request.description(),
                request.price(),
                request.category(),
                request.brand(),
                request.stock()
        ));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@RequestHeader(value = "X-User-Roles", required = false) String roles,
                                                         @PathVariable Long id,
                                                         @RequestBody UpdateProductRequest request) {
        requireMerchant(roles);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product details are required");
        }

        return ResponseEntity.ok(productService.updateProduct(
                id,
                request.name(),
                request.description(),
                request.price(),
                request.category(),
                request.brand(),
                request.stock()
        ));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> uploadImage(@RequestHeader(value = "X-User-Roles", required = false) String roles,
                                                       @PathVariable Long id,
                                                       @RequestParam("image") MultipartFile image) {
        requireMerchant(roles);

        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addImage(id, image));
    }

    @DeleteMapping("/{id}/images/{filename}")
    public ResponseEntity<ProductResponse> deleteImage(@RequestHeader(value = "X-User-Roles", required = false) String roles,
                                                       @PathVariable Long id,
                                                       @PathVariable String filename) {
        requireMerchant(roles);

        return ResponseEntity.ok(productService.removeImage(id, filename));
    }

    private void requireMerchant(String roles) {
        if (roles == null || List.of(roles.split(",")).stream().map(String::trim).noneMatch("MERCHANT"::equals)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Merchant role required");
        }
    }

    record DeductStockRequest(@Min(1) int quantity) {}
    record RestoreStockRequest(@Min(1) int quantity) {}
    record SetStockRequest(@Min(0) int stock) {}
    record UpdateProductRequest(
            String name,
            String description,
            BigDecimal price,
            String category,
            String brand,
            @Min(0) int stock
    ) {}
}
