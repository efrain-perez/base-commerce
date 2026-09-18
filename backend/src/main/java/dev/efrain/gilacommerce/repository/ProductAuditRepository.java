package dev.efrain.gilacommerce.repository;

import dev.efrain.gilacommerce.entity.ProductAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAuditRepository extends JpaRepository<ProductAudit, Long> {

    Page<ProductAudit> findByProductIdOrderByRecordedAtDesc(Long productId, Pageable pageable);
}
