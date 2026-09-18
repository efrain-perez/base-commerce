package dev.efrain.gilacommerce.dto;

import dev.efrain.gilacommerce.entity.ProductAudit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductAuditResponse(
        Long id,
        String action,
        Long importJobId,
        String sku,
        String name,
        String description,
        String category,
        BigDecimal price,
        BigDecimal weightKg,
        Long version,
        LocalDateTime recordedAt) {

    public static ProductAuditResponse from(ProductAudit audit) {
        return new ProductAuditResponse(
                audit.getId(),
                audit.getAction().name(),
                audit.getImportJobId(),
                audit.getSku(),
                audit.getName(),
                audit.getDescription(),
                audit.getCategory(),
                audit.getPrice(),
                audit.getWeightKg(),
                audit.getVersion(),
                audit.getRecordedAt());
    }
}
