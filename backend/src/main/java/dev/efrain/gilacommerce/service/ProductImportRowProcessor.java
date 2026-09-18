package dev.efrain.gilacommerce.service;

import dev.efrain.gilacommerce.entity.Product;
import dev.efrain.gilacommerce.entity.ProductAuditAction;
import dev.efrain.gilacommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ProductImportRowProcessor {

    private final ProductRepository productRepository;
    private final ProductAuditService productAuditService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsert(ProductCsvRow row, Long importJobId) {
        Product product = productRepository.findBySku(row.sku()).orElseGet(Product::new);
        boolean isNew = product.getId() == null;
        if (isNew) {
            product.setSku(row.sku());
        }
        product.setName(row.name());
        product.setDescription(row.description());
        product.setCategory(row.category());
        product.setPrice(row.price());
        product.setStock(row.stock());
        product.setWeightKg(row.weightKg());
        Product saved = productRepository.saveAndFlush(product);
        productAuditService.record(saved, isNew ? ProductAuditAction.CREATED : ProductAuditAction.UPDATED, importJobId);
    }
}
