package dev.efrain.gilacommerce.service;

import dev.efrain.gilacommerce.dto.ImportJobDetailResponse;
import dev.efrain.gilacommerce.dto.ProductCreateRequest;
import dev.efrain.gilacommerce.dto.ProductUpdateRequest;
import dev.efrain.gilacommerce.entity.Product;
import dev.efrain.gilacommerce.entity.ProductAudit;
import dev.efrain.gilacommerce.entity.ProductAuditAction;
import dev.efrain.gilacommerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductAuditIT {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductImportService productImportService;

    @Autowired
    private ProductRepository productRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    @Test
    void createUpdateAndDeleteEachRecordAnAuditSnapshot() {
        String sku = "AUDIT-" + suffix;
        Product created = productService.create(new ProductCreateRequest(
                sku, "Original Name", null, null, new BigDecimal("10.00"), 5, null));

        productService.update(created.getId(), new ProductUpdateRequest(
                "Updated Name", "desc", "Category", new BigDecimal("15.00"), 3, null));

        productService.softDelete(created.getId());

        List<ProductAudit> history = productService
                .getAuditHistory(created.getId(), PageRequest.of(0, 10))
                .getContent();

        assertThat(history).hasSize(3);
        assertThat(history).extracting(ProductAudit::getAction)
                .containsExactly(ProductAuditAction.DELETED, ProductAuditAction.UPDATED, ProductAuditAction.CREATED);

        ProductAudit createdSnapshot = history.get(2);
        assertThat(createdSnapshot.getName()).isEqualTo("Original Name");
        assertThat(createdSnapshot.getPrice()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(createdSnapshot.getImportJobId()).isNull();

        ProductAudit updatedSnapshot = history.get(1);
        assertThat(updatedSnapshot.getName()).isEqualTo("Updated Name");
        assertThat(updatedSnapshot.getVersion()).isGreaterThan(createdSnapshot.getVersion());

        ProductAudit deletedSnapshot = history.get(0);
        assertThat(deletedSnapshot.getName()).isEqualTo("Updated Name");
    }

    @Test
    void csvImportRecordsAuditEntriesStampedWithTheImportJobId() {
        String sku = "AUDIT-IMPORT-" + suffix;
        String csv = String.join("\n",
                "name,sku,description,category,price,stock,weight_kg",
                "Imported Product," + sku + ",desc,Category,20.00,4,0.5");
        MockMultipartFile file = new MockMultipartFile(
                "file", "audit.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportJobDetailResponse result = productImportService.importCsv(file);
        Product product = productRepository.findBySku(sku).orElseThrow();

        List<ProductAudit> history = productService
                .getAuditHistory(product.getId(), PageRequest.of(0, 10))
                .getContent();

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getAction()).isEqualTo(ProductAuditAction.CREATED);
        assertThat(history.get(0).getImportJobId()).isEqualTo(result.job().id());
    }
}
