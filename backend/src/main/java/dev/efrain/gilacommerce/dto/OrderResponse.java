package dev.efrain.gilacommerce.dto;

import dev.efrain.gilacommerce.entity.Order;
import dev.efrain.gilacommerce.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String status,
        LocalDateTime createdAt,
        List<OrderItemResponse> items,
        BigDecimal orderTotal) {

    public static OrderResponse from(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream().map(OrderItemResponse::from).toList();
        BigDecimal orderTotal = itemResponses.stream()
                .map(OrderItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderResponse(order.getId(), order.getStatus().name(), order.getCreatedAt(), itemResponses, orderTotal);
    }
}
