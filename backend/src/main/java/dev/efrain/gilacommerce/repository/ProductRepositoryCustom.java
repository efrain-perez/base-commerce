package dev.efrain.gilacommerce.repository;

import dev.efrain.gilacommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {

    Page<Product> search(String name, Pageable pageable);
}
