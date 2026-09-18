package dev.efrain.gilacommerce.dto;

import dev.efrain.gilacommerce.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String name,
        BigDecimal priceAtPurchase,
        Long versionAtPurchase,
        Integer quantity,
        BigDecimal lineTotal) {

    public static OrderItemResponse from(OrderItem item) {
        BigDecimal lineTotal = item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new OrderItemResponse(
                item.getProductId(),
                item.getName(),
                item.getPriceAtPurchase(),
                item.getVersionAtPurchase(),
                item.getQuantity(),
                lineTotal);
    }
}
