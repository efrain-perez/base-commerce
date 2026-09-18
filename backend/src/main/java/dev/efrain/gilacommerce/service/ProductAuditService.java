package dev.efrain.gilacommerce.service;

import dev.efrain.gilacommerce.entity.Product;
import dev.efrain.gilacommerce.entity.ProductAudit;
import dev.efrain.gilacommerce.entity.ProductAuditAction;
import dev.efrain.gilacommerce.repository.ProductAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class ProductAuditService {

    private final ProductAuditRepository productAuditRepository;

    void record(Product product, ProductAuditAction action, Long importJobId) {
        ProductAudit audit = new ProductAudit();
        audit.setProductId(product.getId());
        audit.setAction(action);
        audit.setImportJobId(importJobId);
        audit.setSku(product.getSku());
        audit.setName(product.getName());
        audit.setDescription(product.getDescription());
        audit.setCategory(product.getCategory());
        audit.setPrice(product.getPrice());
        audit.setWeightKg(product.getWeightKg());
        audit.setVersion(product.getVersion());
        productAuditRepository.save(audit);
    }
}
