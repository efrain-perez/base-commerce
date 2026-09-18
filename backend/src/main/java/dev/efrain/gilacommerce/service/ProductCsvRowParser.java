package dev.efrain.gilacommerce.service;

import org.apache.commons.csv.CSVRecord;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

final class ProductCsvRowParser {

    private ProductCsvRowParser() {
    }

    static ProductCsvRow parse(CSVRecord record) {
        String sku = requireNonBlank(record.get("sku"), "sku");
        String name = requireNonBlank(record.get("name"), "name");
        String description = blankToNull(record.get("description"));
        String category = blankToNull(record.get("category"));
        BigDecimal price = parsePrice(record.get("price"));
        int stock = parseStock(record.get("stock"));
        BigDecimal weightKg = parseOptionalDecimal(record.get("weight_kg"), "weight_kg");
        return new ProductCsvRow(sku, name, description, category, price, stock, weightKg);
    }

    private static String requireNonBlank(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static BigDecimal parsePrice(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("price is required");
        }
        BigDecimal price;
        try {
            price = new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("price must be a valid decimal number");
        }
        if (price.signum() < 0) {
            throw new IllegalArgumentException("price must not be negative");
        }
        return price;
    }

    private static int parseStock(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("stock is required");
        }
        int stock;
        try {
            stock = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("stock must be a valid integer");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("stock must not be negative");
        }
        return stock;
    }

    private static BigDecimal parseOptionalDecimal(String raw, String field) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " must be a valid decimal number");
        }
    }
}
