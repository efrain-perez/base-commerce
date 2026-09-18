package dev.efrain.gilacommerce.service;

import dev.efrain.gilacommerce.dto.OrderResponse;
import dev.efrain.gilacommerce.entity.Order;
import dev.efrain.gilacommerce.entity.OrderItem;
import dev.efrain.gilacommerce.exception.ResourceNotFoundException;
import dev.efrain.gilacommerce.repository.OrderItemRepository;
import dev.efrain.gilacommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + id + " not found"));
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(id);
        return OrderResponse.from(order, items);
    }
}
