package dev.efrain.gilacommerce.service;

import dev.efrain.gilacommerce.entity.Cart;
import dev.efrain.gilacommerce.entity.CartStatus;
import dev.efrain.gilacommerce.repository.CartItemRepository;
import dev.efrain.gilacommerce.repository.CartRepository;
import dev.efrain.gilacommerce.repository.ProductRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @Mock
    private HttpServletResponse response;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, cartItemRepository, productRepository, productService);
    }

    @Test
    void reusesAnExistingActiveCartWithoutIssuingANewCookie() {
        UUID cartId = UUID.randomUUID();
        Cart existing = new Cart();
        existing.setId(cartId);
        existing.setStatus(CartStatus.ACTIVE);
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(existing));

        Cart result = cartService.resolveOrCreateActiveCart(cartId.toString(), response);

        assertThat(result.getId()).isEqualTo(cartId);
        verify(response, never()).addHeader(any(), any());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void createsANewCartWhenCookieIsMissing() {
        when(cartRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Cart result = cartService.resolveOrCreateActiveCart(null, response);

        assertThat(result.getStatus()).isEqualTo(CartStatus.ACTIVE);
        assertNewCookieIssued();
    }

    @Test
    void createsANewCartWhenCookieIsNotAValidUuid() {
        when(cartRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        cartService.resolveOrCreateActiveCart("not-a-uuid", response);

        verify(cartRepository, never()).findById(any());
        assertNewCookieIssued();
    }

    @Test
    void createsANewCartWhenCookiePointsAtANonexistentCart() {
        UUID cartId = UUID.randomUUID();
        when(cartRepository.findById(cartId)).thenReturn(Optional.empty());
        when(cartRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        cartService.resolveOrCreateActiveCart(cartId.toString(), response);

        assertNewCookieIssued();
    }

    @Test
    void createsANewCartWhenCookiePointsAtACheckedOutCart() {
        UUID cartId = UUID.randomUUID();
        Cart checkedOut = new Cart();
        checkedOut.setId(cartId);
        checkedOut.setStatus(CartStatus.CHECKED_OUT);
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(checkedOut));
        when(cartRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Cart result = cartService.resolveOrCreateActiveCart(cartId.toString(), response);

        assertThat(result.getId()).isNotEqualTo(cartId);
        assertNewCookieIssued();
    }

    private void assertNewCookieIssued() {
        ArgumentCaptor<String> headerValue = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(org.mockito.ArgumentMatchers.eq(HttpHeaders.SET_COOKIE), headerValue.capture());
        String cookie = headerValue.getValue();
        assertThat(cookie).contains("HttpOnly");
        assertThat(cookie).contains("SameSite=Lax");
        assertThat(cookie).contains("Path=/");
    }
}
