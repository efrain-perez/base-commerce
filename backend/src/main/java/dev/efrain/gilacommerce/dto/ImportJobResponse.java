package dev.efrain.gilacommerce.dto;

import dev.efrain.gilacommerce.entity.ImportJob;

import java.time.LocalDateTime;

public record ImportJobResponse(
        Long id,
        String fileName,
        LocalDateTime executedAt,
        int totalRows,
        int successCount,
        int failureCount,
        String status) {

    public static ImportJobResponse from(ImportJob job) {
        return new ImportJobResponse(
                job.getId(),
                job.getFileName(),
                job.getExecutedAt(),
                job.getTotalRows(),
                job.getSuccessCount(),
                job.getFailureCount(),
                job.getStatus().name());
    }
}
