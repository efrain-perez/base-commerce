package dev.efrain.gilacommerce.service;

import dev.efrain.gilacommerce.dto.ProductCreateRequest;
import dev.efrain.gilacommerce.dto.ProductUpdateRequest;
import dev.efrain.gilacommerce.entity.Product;
import dev.efrain.gilacommerce.entity.ProductAudit;
import dev.efrain.gilacommerce.entity.ProductAuditAction;
import dev.efrain.gilacommerce.exception.DuplicateSkuException;
import dev.efrain.gilacommerce.exception.ResourceNotFoundException;
import dev.efrain.gilacommerce.repository.ProductAuditRepository;
import dev.efrain.gilacommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductAuditRepository productAuditRepository;
    private final ProductAuditService productAuditService;

    @Transactional
    public Product create(ProductCreateRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateSkuException("A product with SKU '" + request.sku() + "' already exists.");
        }
        Product product = new Product();
        product.setSku(request.sku());
        applyFields(product, request.name(), request.description(), request.category(),
                request.price(), request.stock(), request.weightKg());
        Product saved = productRepository.saveAndFlush(product);
        productAuditService.record(saved, ProductAuditAction.CREATED, null);
        return saved;
    }

    @Transactional(readOnly = true)
    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public Page<Product> search(String name, Pageable pageable) {
        String pattern = StringUtils.hasText(name) ? name.trim() : null;
        return productRepository.search(pattern, pageable);
    }

    @Transactional
    public Product update(Long id, ProductUpdateRequest request) {
        Product product = getById(id);
        applyFields(product, request.name(), request.description(), request.category(),
                request.price(), request.stock(), request.weightKg());
        Product saved = productRepository.saveAndFlush(product);
        productAuditService.record(saved, ProductAuditAction.UPDATED, null);
        return saved;
    }

    @Transactional
    public void softDelete(Long id) {
        Product product = getById(id);
        product.setDeletedAt(LocalDateTime.now());
        Product saved = productRepository.saveAndFlush(product);
        productAuditService.record(saved, ProductAuditAction.DELETED, null);
    }

    @Transactional(readOnly = true)
    public Page<ProductAudit> getAuditHistory(Long productId, Pageable pageable) {
        return productAuditRepository.findByProductIdOrderByRecordedAtDesc(productId, pageable);
    }

    private void applyFields(Product product, String name, String description, String category,
            BigDecimal price, Integer stock, BigDecimal weightKg) {
        product.setName(name);
        product.setDescription(description);
        product.setCategory(category);
        product.setPrice(price);
        product.setStock(stock);
        product.setWeightKg(weightKg);
    }
}
