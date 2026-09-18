package dev.efrain.gilacommerce.dto;

import dev.efrain.gilacommerce.entity.CartItem;
import dev.efrain.gilacommerce.entity.Product;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String sku,
        String name,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal) {

    public static CartItemResponse from(CartItem item, Product product) {
        BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getPrice(),
                item.getQuantity(),
                lineTotal);
    }
}
