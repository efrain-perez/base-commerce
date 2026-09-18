package dev.efrain.gilacommerce.controller;

import dev.efrain.gilacommerce.dto.ImportJobDetailResponse;
import dev.efrain.gilacommerce.dto.ProductAuditResponse;
import dev.efrain.gilacommerce.dto.ProductCreateRequest;
import dev.efrain.gilacommerce.dto.ProductResponse;
import dev.efrain.gilacommerce.dto.ProductUpdateRequest;
import dev.efrain.gilacommerce.service.ProductImportService;
import dev.efrain.gilacommerce.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductImportService productImportService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductCreateRequest request) {
        return ProductResponse.from(productService.create(request));
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return ProductResponse.from(productService.getById(id));
    }

    @GetMapping
    public Page<ProductResponse> search(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20) Pageable pageable) {
        return productService.search(name, pageable).map(ProductResponse::from);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return ProductResponse.from(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.softDelete(id);
    }

    @GetMapping("/{id}/audit")
    public Page<ProductAuditResponse> getAuditHistory(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        return productService.getAuditHistory(id, pageable).map(ProductAuditResponse::from);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportJobDetailResponse> importCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file must not be empty");
        }
        ImportJobDetailResponse result = productImportService.importCsv(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/import-jobs/" + result.job().id()))
                .body(result);
    }
}
