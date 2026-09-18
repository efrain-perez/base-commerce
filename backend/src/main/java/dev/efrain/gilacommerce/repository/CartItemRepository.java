package dev.efrain.gilacommerce.repository;

import dev.efrain.gilacommerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartIdOrderByAddedAtAsc(UUID cartId);

    Optional<CartItem> findByCartIdAndProductId(UUID cartId, Long productId);

    void deleteByCartId(UUID cartId);
}
