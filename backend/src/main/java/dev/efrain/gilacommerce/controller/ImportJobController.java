package dev.efrain.gilacommerce.controller;

import dev.efrain.gilacommerce.dto.ImportJobDetailResponse;
import dev.efrain.gilacommerce.dto.ImportJobResponse;
import dev.efrain.gilacommerce.service.ImportJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/import-jobs")
@RequiredArgsConstructor
public class ImportJobController {

    private final ImportJobService importJobService;

    @GetMapping
    public Page<ImportJobResponse> list(
            @PageableDefault(size = 20, sort = "executedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return importJobService.list(pageable).map(ImportJobResponse::from);
    }

    @GetMapping("/{id}")
    public ImportJobDetailResponse getById(@PathVariable Long id) {
        return importJobService.getDetail(id);
    }
}
