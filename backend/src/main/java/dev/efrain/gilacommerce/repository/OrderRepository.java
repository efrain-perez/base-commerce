package dev.efrain.gilacommerce.repository;

import dev.efrain.gilacommerce.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
