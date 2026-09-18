package dev.efrain.gilacommerce.service;

import dev.efrain.gilacommerce.dto.OrderResponse;
import dev.efrain.gilacommerce.entity.Cart;
import dev.efrain.gilacommerce.entity.CartItem;
import dev.efrain.gilacommerce.entity.CartStatus;
import dev.efrain.gilacommerce.entity.Product;
import dev.efrain.gilacommerce.exception.EmptyCartException;
import dev.efrain.gilacommerce.exception.InsufficientStockException;
import dev.efrain.gilacommerce.repository.CartItemRepository;
import dev.efrain.gilacommerce.repository.CartRepository;
import dev.efrain.gilacommerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CheckoutServiceIT {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    @Test
    void insufficientStockOnOneLineRollsBackTheWholeCheckout() {
        Product productA = createProduct("CHK-A-" + suffix, 2);
        Product productB = createProduct("CHK-B-" + suffix, 5);
        Cart cart = createActiveCart();
        addCartItem(cart.getId(), productA.getId(), 2);
        addCartItem(cart.getId(), productB.getId(), 10);

        assertThatThrownBy(() -> checkoutService.checkout(cart.getId().toString(), new MockHttpServletResponse()))
                .isInstanceOf(InsufficientStockException.class);

        Product reloadedA = productRepository.findById(productA.getId()).orElseThrow();
        Product reloadedB = productRepository.findById(productB.getId()).orElseThrow();
        assertThat(reloadedA.getStock()).isEqualTo(2);
        assertThat(reloadedB.getStock()).isEqualTo(5);
        assertThat(cartItemRepository.findByCartIdOrderByAddedAtAsc(cart.getId())).hasSize(2);
        assertThat(cartRepository.findById(cart.getId()).orElseThrow().getStatus()).isEqualTo(CartStatus.ACTIVE);
    }

    @Test
    void sufficientStockOnAllLinesCompletesTheCheckout() {
        Product productA = createProduct("CHK-C-" + suffix, 5);
        Product productB = createProduct("CHK-D-" + suffix, 5);
        Cart cart = createActiveCart();
        addCartItem(cart.getId(), productA.getId(), 2);
        addCartItem(cart.getId(), productB.getId(), 3);

        OrderResponse result = checkoutService.checkout(cart.getId().toString(), new MockHttpServletResponse());

        assertThat(result.items()).hasSize(2);
        Product reloadedA = productRepository.findById(productA.getId()).orElseThrow();
        Product reloadedB = productRepository.findById(productB.getId()).orElseThrow();
        assertThat(reloadedA.getStock()).isEqualTo(3);
        assertThat(reloadedB.getStock()).isEqualTo(2);
        assertThat(cartItemRepository.findByCartIdOrderByAddedAtAsc(cart.getId())).isEmpty();
        assertThat(cartRepository.findById(cart.getId()).orElseThrow().getStatus()).isEqualTo(CartStatus.CHECKED_OUT);
    }

    @Test
    void checkoutOnAnEmptyCartThrows() {
        Cart cart = createActiveCart();

        assertThatThrownBy(() -> checkoutService.checkout(cart.getId().toString(), new MockHttpServletResponse()))
                .isInstanceOf(EmptyCartException.class);
    }

    private Product createProduct(String sku, int stock) {
        Product product = new Product();
        product.setSku(sku);
        product.setName("Product " + sku);
        product.setPrice(new BigDecimal("10.00"));
        product.setStock(stock);
        return productRepository.save(product);
    }

    private Cart createActiveCart() {
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setStatus(CartStatus.ACTIVE);
        return cartRepository.save(cart);
    }

    private void addCartItem(UUID cartId, Long productId, int quantity) {
        CartItem item = new CartItem();
        item.setCartId(cartId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }
}
