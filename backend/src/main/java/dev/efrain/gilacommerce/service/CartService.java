package dev.efrain.gilacommerce.service;

import dev.efrain.gilacommerce.dto.CartResponse;
import dev.efrain.gilacommerce.dto.RemovedCartItemResponse;
import dev.efrain.gilacommerce.entity.Cart;
import dev.efrain.gilacommerce.entity.CartItem;
import dev.efrain.gilacommerce.entity.CartStatus;
import dev.efrain.gilacommerce.entity.Product;
import dev.efrain.gilacommerce.exception.InsufficientStockException;
import dev.efrain.gilacommerce.exception.ResourceNotFoundException;
import dev.efrain.gilacommerce.repository.CartItemRepository;
import dev.efrain.gilacommerce.repository.CartRepository;
import dev.efrain.gilacommerce.repository.ProductRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    public static final String CART_COOKIE_NAME = "cartId";
    private static final Duration CART_COOKIE_MAX_AGE = Duration.ofDays(30);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    @Transactional
    public CartResponse getCart(String cartIdCookieValue, HttpServletResponse response) {
        Cart cart = resolveOrCreateActiveCart(cartIdCookieValue, response);
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse addItem(String cartIdCookieValue, HttpServletResponse response, Long productId, int quantity) {
        Cart cart = resolveOrCreateActiveCart(cartIdCookieValue, response);
        Product product = productService.getById(productId);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseGet(() -> newItem(cart.getId(), productId));
        int newQuantity = (item.getQuantity() == null ? 0 : item.getQuantity()) + quantity;
        requireSufficientStock(product, newQuantity);
        item.setQuantity(newQuantity);
        cartItemRepository.save(item);
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(
            String cartIdCookieValue, HttpServletResponse response, Long productId, int quantity) {
        Cart cart = resolveOrCreateActiveCart(cartIdCookieValue, response);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " is not in the cart"));
        Product product = productService.getById(productId);
        requireSufficientStock(product, quantity);
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(String cartIdCookieValue, HttpServletResponse response, Long productId) {
        Cart cart = resolveOrCreateActiveCart(cartIdCookieValue, response);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " is not in the cart"));
        cartItemRepository.delete(item);
        return buildCartResponse(cart);
    }

    @Transactional
    Cart resolveOrCreateActiveCart(String cartIdCookieValue, HttpServletResponse response) {
        Optional<Cart> existing = parseUuid(cartIdCookieValue)
                .flatMap(cartRepository::findById)
                .filter(c -> c.getStatus() == CartStatus.ACTIVE);
        if (existing.isPresent()) {
            return existing.get();
        }
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setStatus(CartStatus.ACTIVE);
        Cart saved = cartRepository.save(cart);
        setCartCookie(response, saved.getId());
        return saved;
    }

    private void requireSufficientStock(Product product, int requestedQuantity) {
        if (product.getStock() < requestedQuantity) {
            throw new InsufficientStockException("Only " + product.getStock() + " unit(s) of '"
                    + product.getName() + "' are available.");
        }
    }

    private CartItem newItem(UUID cartId, Long productId) {
        CartItem item = new CartItem();
        item.setCartId(cartId);
        item.setProductId(productId);
        item.setQuantity(0);
        return item;
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartIdOrderByAddedAtAsc(cart.getId());
        Map<Long, Product> productsById = productRepository.findAllById(
                items.stream().map(CartItem::getProductId).toList()
        ).stream().collect(Collectors.toMap(Product::getId, product -> product));

        List<CartItem> deletedProductItems = items.stream()
                .filter(item -> !productsById.containsKey(item.getProductId()))
                .toList();
        List<RemovedCartItemResponse> removedItems = buildRemovedItems(deletedProductItems);
        if (!deletedProductItems.isEmpty()) {
            cartItemRepository.deleteAll(deletedProductItems);
        }

        List<CartItem> remainingItems = items.stream()
                .filter(item -> !deletedProductItems.contains(item))
                .toList();

        return CartResponse.from(cart, remainingItems, productsById, removedItems);
    }

    private List<RemovedCartItemResponse> buildRemovedItems(List<CartItem> deletedProductItems) {
        if (deletedProductItems.isEmpty()) {
            return List.of();
        }
        Map<Long, Product> deletedProducts = productRepository.findAllByIdIncludingDeleted(
                deletedProductItems.stream().map(CartItem::getProductId).toList()
        ).stream().collect(Collectors.toMap(Product::getId, product -> product));
        return deletedProductItems.stream()
                .map(item -> new RemovedCartItemResponse(
                        item.getProductId(),
                        deletedProducts.containsKey(item.getProductId())
                                ? deletedProducts.get(item.getProductId()).getName()
                                : "Unknown product"))
                .toList();
    }

    private Optional<UUID> parseUuid(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private void setCartCookie(HttpServletResponse response, UUID cartId) {
        ResponseCookie cookie = ResponseCookie.from(CART_COOKIE_NAME, cartId.toString())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(CART_COOKIE_MAX_AGE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
