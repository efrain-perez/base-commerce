package dev.efrain.gilacommerce.service;

import dev.efrain.gilacommerce.dto.OrderResponse;
import dev.efrain.gilacommerce.entity.Cart;
import dev.efrain.gilacommerce.entity.CartItem;
import dev.efrain.gilacommerce.entity.CartStatus;
import dev.efrain.gilacommerce.entity.Order;
import dev.efrain.gilacommerce.entity.OrderItem;
import dev.efrain.gilacommerce.entity.OrderStatus;
import dev.efrain.gilacommerce.entity.Product;
import dev.efrain.gilacommerce.exception.EmptyCartException;
import dev.efrain.gilacommerce.exception.InsufficientStockException;
import dev.efrain.gilacommerce.repository.CartItemRepository;
import dev.efrain.gilacommerce.repository.CartRepository;
import dev.efrain.gilacommerce.repository.OrderItemRepository;
import dev.efrain.gilacommerce.repository.OrderRepository;
import dev.efrain.gilacommerce.repository.ProductRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartService cartService;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public OrderResponse checkout(String cartIdCookieValue, HttpServletResponse response) {
        Cart cart = cartService.resolveOrCreateActiveCart(cartIdCookieValue, response);
        List<CartItem> items = cartItemRepository.findByCartIdOrderByAddedAtAsc(cart.getId());
        if (items.isEmpty()) {
            throw new EmptyCartException("Cannot checkout an empty cart.");
        }

        Order order = new Order();
        order.setCartId(cart.getId());
        order.setStatus(OrderStatus.COMPLETED);
        Order savedOrder = orderRepository.saveAndFlush(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem item : items) {
            Product product = productService.getById(item.getProductId());
            if (product.getStock() < item.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for '" + product.getName()
                        + "' (sku " + product.getSku() + "): requested " + item.getQuantity()
                        + ", available " + product.getStock());
            }
            product.setStock(product.getStock() - item.getQuantity());
            Product savedProduct = productRepository.saveAndFlush(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setProductId(savedProduct.getId());
            orderItem.setName(savedProduct.getName());
            orderItem.setPriceAtPurchase(savedProduct.getPrice());
            orderItem.setVersionAtPurchase(savedProduct.getVersion());
            orderItem.setQuantity(item.getQuantity());
            orderItems.add(orderItem);
        }
        orderItemRepository.saveAll(orderItems);

        cartItemRepository.deleteByCartId(cart.getId());
        cart.setStatus(CartStatus.CHECKED_OUT);
        cartRepository.save(cart);

        return OrderResponse.from(savedOrder, orderItems);
    }
}
