package dev.efrain.gilacommerce.service;

import dev.efrain.gilacommerce.dto.ProductCreateRequest;
import dev.efrain.gilacommerce.dto.ProductUpdateRequest;
import dev.efrain.gilacommerce.entity.Product;
import dev.efrain.gilacommerce.entity.ProductAuditAction;
import dev.efrain.gilacommerce.exception.DuplicateSkuException;
import dev.efrain.gilacommerce.exception.ResourceNotFoundException;
import dev.efrain.gilacommerce.repository.ProductAuditRepository;
import dev.efrain.gilacommerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductAuditRepository productAuditRepository;

    @Mock
    private ProductAuditService productAuditService;

    private ProductService productService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, productAuditRepository, productAuditService);
    }

    @Test
    void createThrowsWhenSkuAlreadyExists() {
        ProductCreateRequest request = new ProductCreateRequest(
                "SKU-1", "Laptop", null, null, new BigDecimal("999.00"), 5, null);
        when(productRepository.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(DuplicateSkuException.class);

        verify(productRepository, never()).saveAndFlush(any());
        verify(productAuditService, never()).record(any(), any(), any());
    }

    @Test
    void createRecordsAnAuditEntry() {
        ProductCreateRequest request = new ProductCreateRequest(
                "SKU-1", "Laptop", null, null, new BigDecimal("999.00"), 5, null);
        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(productRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Product created = productService.create(request);

        verify(productAuditService).record(eq(created), eq(ProductAuditAction.CREATED), isNull());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateNeverChangesSkuAndRecordsAnAuditEntry() {
        Product existing = new Product();
        existing.setId(1L);
        existing.setSku("SKU-ORIGINAL");
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProductUpdateRequest request = new ProductUpdateRequest(
                "New Name", "New description", "New category", new BigDecimal("50.00"), 10, null);
        productService.update(1L, request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getSku()).isEqualTo("SKU-ORIGINAL");
        assertThat(captor.getValue().getName()).isEqualTo("New Name");
        verify(productAuditService).record(eq(captor.getValue()), eq(ProductAuditAction.UPDATED), isNull());
    }

    @Test
    void softDeleteSetsDeletedAtInsteadOfDeletingAndRecordsAnAuditEntry() {
        Product existing = new Product();
        existing.setId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        productService.softDelete(1L);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        verify(productRepository, never()).delete(any());
        verify(productRepository, never()).deleteById(any());
        verify(productAuditService).record(eq(captor.getValue()), eq(ProductAuditAction.DELETED), isNull());
    }
}
