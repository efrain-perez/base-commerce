package dev.efrain.gilacommerce.repository;

import dev.efrain.gilacommerce.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
}
