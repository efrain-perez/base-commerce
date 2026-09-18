package dev.efrain.gilacommerce.dto;

import dev.efrain.gilacommerce.entity.ImportJob;
import dev.efrain.gilacommerce.entity.ImportJobError;

import java.util.List;

public record ImportJobDetailResponse(
        ImportJobResponse job,
        List<ImportJobErrorResponse> errors) {

    public static ImportJobDetailResponse from(ImportJob job, List<ImportJobError> errors) {
        return new ImportJobDetailResponse(
                ImportJobResponse.from(job),
                errors.stream().map(ImportJobErrorResponse::from).toList());
    }
}
