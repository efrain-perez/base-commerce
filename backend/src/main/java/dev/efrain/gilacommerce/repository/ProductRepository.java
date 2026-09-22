package dev.efrain.gilacommerce.repository;

import dev.efrain.gilacommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    boolean existsBySku(String sku);

    Optional<Product> findBySku(String sku);

    @Query(value = "SELECT * FROM product WHERE id IN (:ids)", nativeQuery = true)
    List<Product> findAllByIdIncludingDeleted(@Param("ids") Collection<Long> ids);
}
