package dev.efrain.gilacommerce.repository;

import dev.efrain.gilacommerce.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    @Test
    void sortsByPriceAscendingAndDescending() {
        saveProduct("SORT-1", "Cheap Item", new BigDecimal("5.00"));
        saveProduct("SORT-2", "Mid Item", new BigDecimal("15.00"));
        saveProduct("SORT-3", "Expensive Item", new BigDecimal("25.00"));

        Page<Product> ascending = productRepository.search(null, PageRequest.of(0, 1000, Sort.by("price").ascending()));
        assertThat(ascending.getContent())
                .filteredOn(p -> p.getSku().startsWith("SORT-") && !p.getSku().startsWith("SORT-NAME"))
                .extracting(Product::getSku)
                .containsExactly("SORT-1", "SORT-2", "SORT-3");

        Page<Product> descending = productRepository.search(null, PageRequest.of(0, 1000, Sort.by("price").descending()));
        assertThat(descending.getContent())
                .filteredOn(p -> p.getSku().startsWith("SORT-") && !p.getSku().startsWith("SORT-NAME"))
                .extracting(Product::getSku)
                .containsExactly("SORT-3", "SORT-2", "SORT-1");
    }

    @Test
    void sortsByName() {
        saveProduct("SORT-NAME-1", "Zebra", new BigDecimal("10.00"));
        saveProduct("SORT-NAME-2", "Apple", new BigDecimal("10.00"));

        Page<Product> results = productRepository.search(null, PageRequest.of(0, 1000, Sort.by("name").ascending()));

        assertThat(results.getContent())
                .filteredOn(p -> p.getSku().startsWith("SORT-NAME"))
                .extracting(Product::getName)
                .containsExactly("Apple", "Zebra");
    }

    @Test
    void unrecognizedSortPropertyFallsBackToIdOrder() {
        Product first = saveProduct("FALLBACK-1", "First", new BigDecimal("10.00"));
        Product second = saveProduct("FALLBACK-2", "Second", new BigDecimal("10.00"));

        Page<Product> results = productRepository.search(
                null, PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "notARealColumn")));

        assertThat(results.getContent())
                .filteredOn(p -> p.getId().equals(first.getId()) || p.getId().equals(second.getId()))
                .extracting(Product::getId)
                .containsExactly(second.getId(), first.getId());
    }

    private Product saveProduct(String sku, String name, boolean deleted) {
        return saveProduct(sku, name, new BigDecimal("10.00"), deleted);
    }

    private Product saveProduct(String sku, String name, BigDecimal price) {
        return saveProduct(sku, name, price, false);
    }

    private Product saveProduct(String sku, String name, BigDecimal price, boolean deleted) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setPrice(price);
        product.setStock(1);
        if (deleted) {
            product.setDeletedAt(LocalDateTime.now());
        }
        return productRepository.save(product);
    }
}
