package dev.efrain.gilacommerce.controller;

import dev.efrain.gilacommerce.dto.CartItemAddRequest;
import dev.efrain.gilacommerce.dto.CartItemQuantityUpdateRequest;
import dev.efrain.gilacommerce.dto.CartResponse;
import dev.efrain.gilacommerce.dto.OrderResponse;
import dev.efrain.gilacommerce.service.CartService;
import dev.efrain.gilacommerce.service.CheckoutService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CheckoutService checkoutService;

    @GetMapping
    public CartResponse getCart(
            @CookieValue(value = CartService.CART_COOKIE_NAME, required = false) String cartId,
            HttpServletResponse response) {
        return cartService.getCart(cartId, response);
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @CookieValue(value = CartService.CART_COOKIE_NAME, required = false) String cartId,
            HttpServletResponse response,
            @Valid @RequestBody CartItemAddRequest request) {
        CartResponse result = cartService.addItem(cartId, response, request.productId(), request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/cart/items/" + request.productId()))
                .body(result);
    }

    @PatchMapping("/items/{productId}")
    public CartResponse updateItemQuantity(
            @CookieValue(value = CartService.CART_COOKIE_NAME, required = false) String cartId,
            HttpServletResponse response,
            @PathVariable Long productId,
            @Valid @RequestBody CartItemQuantityUpdateRequest request) {
        return cartService.updateItemQuantity(cartId, response, productId, request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(
            @CookieValue(value = CartService.CART_COOKIE_NAME, required = false) String cartId,
            HttpServletResponse response,
            @PathVariable Long productId) {
        cartService.removeItem(cartId, response, productId);
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @CookieValue(value = CartService.CART_COOKIE_NAME, required = false) String cartId,
            HttpServletResponse response) {
        OrderResponse result = checkoutService.checkout(cartId, response);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/orders/" + result.id()))
                .body(result);
    }
}
