package com.example.product.service.impl;

import com.example.lib.exception.InsufficientStockException;
import com.example.product.dto.ProductFiltersResponse;
import com.example.product.dto.ProductResponse;
import com.example.product.entity.Product;
import com.example.product.exception.ProductNotFoundException;
import com.example.product.repository.ProductRepository;
import com.example.product.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Value("${product.images.path:/app/images}")
    private String imagesPath;

    @Override
    public Page<ProductResponse> getProducts(
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
    ) {
        if (ids != null && !ids.isEmpty()) {
            log.debug("getProducts ids={}", ids);
            var products = productRepository.findAllById(ids).stream()
                    .map(ProductResponse::from)
                    .toList();
            return new PageImpl<>(products);
        }
        log.debug(
                "getProducts category={} query={} brands={} minRating={} minPrice={} maxPrice={} inStock={} sort={} page={} limit={}",
                category,
                query,
                brands,
                minRating,
                minPrice,
                maxPrice,
                inStock,
                sort,
                page,
                limit
        );
        var pageable = PageRequest.of(page, limit, toSort(sort));
        var products = productRepository.findAll(
                withFilters(category, query, brands, minRating, minPrice, maxPrice, inStock),
                pageable
        );
        log.debug("getProducts returned {} of {} total", products.getNumberOfElements(), products.getTotalElements());
        return products.map(ProductResponse::from);
    }

    private Specification<Product> withFilters(
            String category,
            String query,
            List<String> brands,
            Double minRating,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock
    ) {
        return Specification
                .where(hasCategory(category))
                .and(matchesQuery(query))
                .and(hasBrandIn(brands))
                .and(hasMinimumRating(minRating))
                .and(hasMinimumPrice(minPrice))
                .and(hasMaximumPrice(maxPrice))
                .and(hasStockAvailable(inStock));
    }

    private Specification<Product> hasStockAvailable(Boolean inStock) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (inStock == null || !inStock) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.greaterThan(root.<Integer>get("stock"), 0);
        };
    }

    private Specification<Product> hasCategory(String category) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (category == null || category.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.<String>get("category")),
                    category.trim().toLowerCase(Locale.ROOT)
            );
        };
    }

    private Specification<Product> matchesQuery(String query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (query == null || query.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            var pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("name")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("brand")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("category")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("description")), pattern)
            );
        };
    }

    private Specification<Product> hasBrandIn(List<String> brands) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (brands == null || brands.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            var normalizedBrands = brands.stream()
                    .filter(brand -> brand != null && !brand.isBlank())
                    .map(brand -> brand.trim().toLowerCase(Locale.ROOT))
                    .toList();

            if (normalizedBrands.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.lower(root.<String>get("brand")).in(normalizedBrands);
        };
    }

    private Specification<Product> hasMinimumRating(Double minRating) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (minRating == null || minRating <= 0) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.greaterThanOrEqualTo(root.<Double>get("rating"), minRating);
        };
    }

    private Specification<Product> hasMinimumPrice(BigDecimal minPrice) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (minPrice == null || minPrice.signum() < 0) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
        };
    }

    private Specification<Product> hasMaximumPrice(BigDecimal maxPrice) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (maxPrice == null || maxPrice.signum() < 0) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    private Sort toSort(String sort) {
        return switch (sort == null ? "relevance" : sort) {
            case "price-asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price-desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "rating-desc" -> Sort.by(Sort.Direction.DESC, "rating")
                    .and(Sort.by(Sort.Direction.DESC, "reviewCount"));
            case "newest" -> Sort.by(Sort.Direction.DESC, "id");
            default -> Sort.by(Sort.Direction.ASC, "id");
        };
    }

    @Override
    public ProductResponse getProductById(Long id) {
        log.debug("getProductById id={}", id);
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    public ProductFiltersResponse getProductFilters() {
        return new ProductFiltersResponse(
                productRepository.findDistinctCategories(),
                productRepository.findDistinctBrands()
        );
    }

    @Override
    @Transactional
    public void deductStock(Long id, int quantity) {
        log.debug("deductStock id={} quantity={}", id, quantity);
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        if (quantity > product.getStock()) {
            throw new InsufficientStockException(id, quantity, product.getStock());
        }
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void restoreStock(Long id, int quantity) {
        log.debug("restoreStock id={} quantity={}", id, quantity);
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setStock(product.getStock() + quantity);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public ProductResponse setStock(Long id, int stock) {
        log.debug("setStock id={} stock={}", id, stock);
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setStock(Math.max(stock, 0));

        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse createProduct(
            String name,
            String description,
            BigDecimal price,
            String category,
            String brand,
            int stock
    ) {
        log.debug("createProduct name={} category={}", name, category);
        var product = new Product();
        product.setName(requiredText(name, "name"));
        product.setDescription(description == null ? "" : description.trim());
        product.setPrice(requiredPositivePrice(price));
        product.setCategory(requiredText(category, "category"));
        product.setBrand(brand == null ? "" : brand.trim());
        product.setStock(Math.max(stock, 0));
        product.setRating(0.0);
        product.setReviewCount(0);

        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(
            Long id,
            String name,
            String description,
            BigDecimal price,
            String category,
            String brand,
            int stock
    ) {
        log.debug("updateProduct id={}", id);
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setName(requiredText(name, "name"));
        product.setDescription(description == null ? "" : description.trim());
        product.setPrice(requiredPositivePrice(price));
        product.setCategory(requiredText(category, "category"));
        product.setBrand(brand == null ? "" : brand.trim());
        product.setStock(Math.max(stock, 0));

        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse addImage(Long id, MultipartFile image) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image is required");
        }

        var contentType = image.getContentType();
        if (contentType == null || !Set.of("image/jpeg", "image/png", "image/webp").contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, and WebP images are supported");
        }

        var extension = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
        var filename = UUID.randomUUID() + extension;
        Path directory = Paths.get(imagesPath).normalize();
        Path destination = directory.resolve(filename).normalize();

        try {
            Files.createDirectories(directory);
            image.transferTo(destination);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Image could not be stored", exception);
        }

        product.getImages().add(filename);
        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse removeImage(Long id, String filename) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (filename == null || filename.isBlank() || filename.contains("..") || filename.contains("/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image filename");
        }

        if (!product.getImages().remove(filename)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found on product");
        }

        Path destination = Paths.get(imagesPath).resolve(filename).normalize();
        try {
            Files.deleteIfExists(destination);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Image could not be deleted", exception);
        }

        return ProductResponse.from(productRepository.save(product));
    }

    private String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }

        return value.trim();
    }

    private BigDecimal requiredPositivePrice(BigDecimal price) {
        if (price == null || price.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price must be a non-negative amount");
        }

        return price;
    }
}
