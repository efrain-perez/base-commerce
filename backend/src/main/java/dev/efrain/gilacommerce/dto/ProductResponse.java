package dev.efrain.gilacommerce.dto;

import dev.efrain.gilacommerce.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        String category,
        BigDecimal price,
        Integer stock,
        BigDecimal weightKg,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getStock(),
                product.getWeightKg(),
                product.getVersion(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
