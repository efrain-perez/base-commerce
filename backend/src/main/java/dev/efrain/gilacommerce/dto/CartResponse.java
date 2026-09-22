package dev.efrain.gilacommerce.dto;

import dev.efrain.gilacommerce.entity.Cart;
import dev.efrain.gilacommerce.entity.CartItem;
import dev.efrain.gilacommerce.entity.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CartResponse(
        UUID id,
        String status,
        List<CartItemResponse> items,
        BigDecimal subtotal,
        List<RemovedCartItemResponse> removedItems,
        boolean hasStockIssues) {

    public static CartResponse from(Cart cart, List<CartItem> items, Map<Long, Product> productsById,
            List<RemovedCartItemResponse> removedItems) {
        List<CartItemResponse> itemResponses = items.stream()
                .filter(item -> productsById.containsKey(item.getProductId()))
                .map(item -> CartItemResponse.from(item, productsById.get(item.getProductId())))
                .toList();
        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean hasStockIssues = itemResponses.stream()
                .anyMatch(item -> item.quantity() > item.availableStock());
        return new CartResponse(cart.getId(), cart.getStatus().name(), itemResponses, subtotal, removedItems, hasStockIssues);
    }
}
