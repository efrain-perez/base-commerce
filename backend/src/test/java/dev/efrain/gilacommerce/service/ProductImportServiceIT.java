package dev.efrain.gilacommerce.service;

import dev.efrain.gilacommerce.dto.ImportJobDetailResponse;
import dev.efrain.gilacommerce.entity.ImportJob;
import dev.efrain.gilacommerce.entity.ImportJobError;
import dev.efrain.gilacommerce.entity.ImportJobStatus;
import dev.efrain.gilacommerce.entity.Product;
import dev.efrain.gilacommerce.repository.ImportJobErrorRepository;
import dev.efrain.gilacommerce.repository.ImportJobRepository;
import dev.efrain.gilacommerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductImportServiceIT {

    @Autowired
    private ProductImportService productImportService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ImportJobRepository importJobRepository;

    @Autowired
    private ImportJobErrorRepository importJobErrorRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    @Test
    void upsertsBySkuAndIsolatesRowFailures() {
        Product existing = new Product();
        existing.setSku("EXIST-" + suffix);
        existing.setName("Existing Old Name");
        existing.setPrice(new BigDecimal("1.00"));
        existing.setStock(1);
        existing = productRepository.save(existing);
        Long initialVersion = existing.getVersion();

        String newSku = "NEW-" + suffix;
        String badSku = "BAD-" + suffix;
        String afterBadSku = "AFTER-BAD-" + suffix;

        String csv = String.join("\n",
                "name,sku,description,category,price,stock,weight_kg",
                "Existing Updated Name,EXIST-" + suffix + ",desc,Category A,2.50,20,1.5",
                "Brand New Widget," + newSku + ",desc,Category B,3.00,7,0.5",
                "Broken Row," + badSku + ",desc,Category C,3.00,not-a-number,0.5",
                "Row After Bad," + afterBadSku + ",desc,Category D,4.00,3,0.2"
        );

        MockMultipartFile file = new MockMultipartFile(
                "file", "products.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse result = productImportService.importCsv(file);

        assertThat(result.job().totalRows()).isEqualTo(4);
        assertThat(result.job().successCount()).isEqualTo(3);
        assertThat(result.job().failureCount()).isEqualTo(1);
        assertThat(result.job().status()).isEqualTo(ImportJobStatus.COMPLETED_WITH_ERRORS.name());
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).errorReason()).isEqualTo("stock must be a valid integer");

        Product updatedExisting = productRepository.findBySku("EXIST-" + suffix).orElseThrow();
        assertThat(updatedExisting.getName()).isEqualTo("Existing Updated Name");
        assertThat(updatedExisting.getVersion()).isGreaterThan(initialVersion);

        Optional<Product> created = productRepository.findBySku(newSku);
        assertThat(created).isPresent();
        assertThat(created.get().getName()).isEqualTo("Brand New Widget");

        assertThat(productRepository.findBySku(badSku)).isEmpty();

        Optional<Product> afterBad = productRepository.findBySku(afterBadSku);
        assertThat(afterBad).isPresent();
        assertThat(afterBad.get().getName()).isEqualTo("Row After Bad");

        ImportJob persistedJob = importJobRepository.findById(result.job().id()).orElseThrow();
        assertThat(persistedJob.getFileName()).isEqualTo("products.csv");
        List<ImportJobError> persistedErrors =
                importJobErrorRepository.findByImportJobIdOrderByRowNumberAsc(persistedJob.getId());
        assertThat(persistedErrors).hasSize(1);
    }

    @Test
    void reimportingARetiredSkuAfterSoftDeleteCreatesAFreshProduct() {
        String sku = "RETIRED-" + suffix;
        Product deleted = new Product();
        deleted.setSku(sku);
        deleted.setName("Old Deleted Product");
        deleted.setPrice(new BigDecimal("5.00"));
        deleted.setStock(1);
        deleted.setDeletedAt(LocalDateTime.now());
        productRepository.save(deleted);

        String csv = String.join("\n",
                "name,sku,description,category,price,stock,weight_kg",
                "Fresh Reused Sku,%s,desc,Category A,9.00,4,0.3".formatted(sku)
        );
        MockMultipartFile file = new MockMultipartFile(
                "file", "reuse.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse result = productImportService.importCsv(file);

        assertThat(result.job().successCount()).isEqualTo(1);
        assertThat(result.job().failureCount()).isEqualTo(0);

        Product active = productRepository.findBySku(sku).orElseThrow();
        assertThat(active.getName()).isEqualTo("Fresh Reused Sku");
    }
}
