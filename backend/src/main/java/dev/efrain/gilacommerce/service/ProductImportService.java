package dev.efrain.gilacommerce.service;

import dev.efrain.gilacommerce.dto.ImportJobDetailResponse;
import dev.efrain.gilacommerce.entity.ImportJob;
import dev.efrain.gilacommerce.entity.ImportJobError;
import dev.efrain.gilacommerce.entity.ImportJobStatus;
import dev.efrain.gilacommerce.repository.ImportJobErrorRepository;
import dev.efrain.gilacommerce.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductImportService {

    private static final String[] HEADERS =
            {"name", "sku", "description", "category", "price", "stock", "weight_kg"};

    private final ProductImportRowProcessor rowProcessor;
    private final ImportJobRepository importJobRepository;
    private final ImportJobErrorRepository importJobErrorRepository;

    public ImportJobDetailResponse importCsv(MultipartFile file) {
        ImportJob job = new ImportJob();
        job.setFileName(file.getOriginalFilename());
        job.setTotalRows(0);
        job.setSuccessCount(0);
        job.setFailureCount(0);
        job.setStatus(ImportJobStatus.COMPLETED);
        job = importJobRepository.save(job);
        Long jobId = job.getId();

        int totalRows = 0;
        int successCount = 0;
        List<ImportJobError> errors = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader(HEADERS)
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .build();
            for (CSVRecord record : format.parse(reader)) {
                totalRows++;
                int rowNumber = (int) record.getRecordNumber() + 1;
                try {
                    ProductCsvRow row = ProductCsvRowParser.parse(record);
                    rowProcessor.upsert(row, jobId);
                    successCount++;
                } catch (Exception ex) {
                    errors.add(buildError(rowNumber, record, ex.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded CSV file", e);
        }

        ImportJobStatus status = errors.isEmpty()
                ? ImportJobStatus.COMPLETED
                : successCount == 0 ? ImportJobStatus.FAILED : ImportJobStatus.COMPLETED_WITH_ERRORS;

        job.setTotalRows(totalRows);
        job.setSuccessCount(successCount);
        job.setFailureCount(errors.size());
        job.setStatus(status);
        job = importJobRepository.save(job);

        for (ImportJobError error : errors) {
            error.setImportJobId(job.getId());
        }
        importJobErrorRepository.saveAll(errors);

        return ImportJobDetailResponse.from(job, errors);
    }

    private ImportJobError buildError(int rowNumber, CSVRecord record, String reason) {
        ImportJobError error = new ImportJobError();
        error.setRowNumber(rowNumber);
        error.setRawData(String.join(",", record.toList()));
        error.setErrorReason(reason != null && reason.length() > 500 ? reason.substring(0, 500) : reason);
        return error;
    }
}
