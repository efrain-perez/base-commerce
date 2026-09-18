package dev.efrain.gilacommerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemAddRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity) {
}
