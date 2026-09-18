package dev.efrain.gilacommerce.service;

import dev.efrain.gilacommerce.dto.ImportJobDetailResponse;
import dev.efrain.gilacommerce.entity.ImportJob;
import dev.efrain.gilacommerce.entity.ImportJobError;
import dev.efrain.gilacommerce.exception.ResourceNotFoundException;
import dev.efrain.gilacommerce.repository.ImportJobErrorRepository;
import dev.efrain.gilacommerce.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportJobService {

    private final ImportJobRepository importJobRepository;
    private final ImportJobErrorRepository importJobErrorRepository;

    @Transactional(readOnly = true)
    public Page<ImportJob> list(Pageable pageable) {
        return importJobRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public ImportJobDetailResponse getDetail(Long id) {
        ImportJob job = importJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Import job " + id + " not found"));
        List<ImportJobError> errors = importJobErrorRepository.findByImportJobIdOrderByRowNumberAsc(id);
        return ImportJobDetailResponse.from(job, errors);
    }
}
