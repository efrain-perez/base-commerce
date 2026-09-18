package dev.efrain.gilacommerce.dto;

import dev.efrain.gilacommerce.entity.ImportJobError;

public record ImportJobErrorResponse(
        Long id,
        int rowNumber,
        String rawData,
        String errorReason) {

    public static ImportJobErrorResponse from(ImportJobError error) {
        return new ImportJobErrorResponse(
                error.getId(),
                error.getRowNumber(),
                error.getRawData(),
                error.getErrorReason());
    }
}
