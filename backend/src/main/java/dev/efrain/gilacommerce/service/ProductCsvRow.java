package dev.efrain.gilacommerce.service;

import java.math.BigDecimal;

record ProductCsvRow(
        String sku,
        String name,
        String description,
        String category,
        BigDecimal price,
        int stock,
        BigDecimal weightKg) {
}
