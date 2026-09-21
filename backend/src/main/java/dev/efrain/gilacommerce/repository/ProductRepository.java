package dev.efrain.gilacommerce.repository;

import dev.efrain.gilacommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    boolean existsBySku(String sku);

    Optional<Product> findBySku(String sku);
}
