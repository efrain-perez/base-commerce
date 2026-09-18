package dev.efrain.gilacommerce.repository;

import dev.efrain.gilacommerce.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
}
