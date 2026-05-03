package com.example.product.repository;

import com.example.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("select distinct p.category from Product p order by p.category")
    List<String> findDistinctCategories();

    @Query("select distinct p.brand from Product p where p.brand is not null order by p.brand")
    List<String> findDistinctBrands();
}
