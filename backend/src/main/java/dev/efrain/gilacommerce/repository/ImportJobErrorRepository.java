package dev.efrain.gilacommerce.repository;

import dev.efrain.gilacommerce.entity.ImportJobError;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportJobErrorRepository extends JpaRepository<ImportJobError, Long> {

    List<ImportJobError> findByImportJobIdOrderByRowNumberAsc(Long importJobId);
}
