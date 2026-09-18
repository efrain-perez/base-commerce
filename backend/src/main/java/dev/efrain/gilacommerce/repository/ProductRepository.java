package dev.efrain.gilacommerce.repository;

import dev.efrain.gilacommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    Optional<Product> findBySku(String sku);

    @Query(value = """
            SELECT * FROM product
            WHERE deleted_at IS NULL
              AND (:name IS NULL OR name ILIKE CONCAT('%', CAST(:name AS varchar), '%'))
            ORDER BY id
            """,
            countQuery = """
            SELECT count(*) FROM product
            WHERE deleted_at IS NULL
              AND (:name IS NULL OR name ILIKE CONCAT('%', CAST(:name AS varchar), '%'))
            """,
            nativeQuery = true)
    Page<Product> search(@Param("name") String name, Pageable pageable);
}
