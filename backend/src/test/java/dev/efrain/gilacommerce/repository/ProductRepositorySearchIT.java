package dev.efrain.gilacommerce.repository;

import dev.efrain.gilacommerce.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductRepositorySearchIT {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void findsCaseInsensitivePartialMatches() {
        saveProduct("SKU-LP1", "Laptop Pro", false);
        saveProduct("SKU-LP2", "Gaming Laptop", false);
        saveProduct("SKU-MS1", "Mouse", false);

        Page<Product> results = productRepository.search("lap", PageRequest.of(0, 10));

        assertThat(results.getContent())
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Laptop Pro", "Gaming Laptop");
    }

    @Test
    void searchIsCaseInsensitive() {
        saveProduct("SKU-LP3", "Laptop Pro", false);

        Page<Product> results = productRepository.search("LAPTOP", PageRequest.of(0, 10));

        assertThat(results.getContent()).extracting(Product::getName).containsExactly("Laptop Pro");
    }

    @Test
    void excludesSoftDeletedProductsFromSearch() {
        saveProduct("SKU-LP4", "Laptop Deleted", true);

        Page<Product> results = productRepository.search("laptop", PageRequest.of(0, 10));

        assertThat(results.getContent()).isEmpty();
    }

    private void saveProduct(String sku, String name, boolean deleted) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setPrice(new BigDecimal("10.00"));
        product.setStock(1);
        if (deleted) {
            product.setDeletedAt(LocalDateTime.now());
        }
        productRepository.save(product);
    }
}
