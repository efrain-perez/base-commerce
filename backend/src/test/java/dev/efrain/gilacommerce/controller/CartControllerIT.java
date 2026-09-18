package dev.efrain.gilacommerce.controller;

import dev.efrain.gilacommerce.dto.CartItemAddRequest;
import dev.efrain.gilacommerce.dto.ProductCreateRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CartControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String uniqueSku(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void cartCookieRoundTripAndCheckoutIssuesAFreshCartAfterward() throws Exception {
        String sku = uniqueSku("CART-CTRL");
        ProductCreateRequest createRequest = new ProductCreateRequest(
                sku, "Cart Test Product", null, null, new BigDecimal("15.00"), 10, null);
        String productJson = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long productId = objectMapper.readTree(productJson).get("id").asLong();

        MvcResult firstVisit = mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();
        String firstCookieValue = extractCookieValue(firstVisit);

        mockMvc.perform(get("/cart").cookie(new Cookie("cartId", firstCookieValue)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.id").value(firstCookieValue));

        CartItemAddRequest addRequest = new CartItemAddRequest(productId, 2);
        mockMvc.perform(post("/cart/items")
                        .cookie(new Cookie("cartId", firstCookieValue))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].productId").value(productId));

        mockMvc.perform(get("/cart").cookie(new Cookie("cartId", firstCookieValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        MvcResult checkoutResult = mockMvc.perform(post("/cart/checkout")
                        .cookie(new Cookie("cartId", firstCookieValue)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        MvcResult postCheckoutAdd = mockMvc.perform(post("/cart/items")
                        .cookie(new Cookie("cartId", firstCookieValue))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        String secondCookieValue = extractCookieValue(postCheckoutAdd);
        assertThat(secondCookieValue).isNotEqualTo(firstCookieValue);
    }

    private String extractCookieValue(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("cartId");
        assertThat(cookie).isNotNull();
        return cookie.getValue();
    }
}
