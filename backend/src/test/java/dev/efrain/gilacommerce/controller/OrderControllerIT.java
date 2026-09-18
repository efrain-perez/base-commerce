package dev.efrain.gilacommerce.controller;

import dev.efrain.gilacommerce.dto.CartItemAddRequest;
import dev.efrain.gilacommerce.dto.ProductCreateRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getByIdReturnsAnOrderCreatedByCheckout() throws Exception {
        String sku = "ORDER-CTRL-" + UUID.randomUUID().toString().substring(0, 8);
        ProductCreateRequest createRequest = new ProductCreateRequest(
                sku, "Order Test Product", null, null, new BigDecimal("8.00"), 5, null);
        String productJson = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long productId = objectMapper.readTree(productJson).get("id").asLong();

        MvcResult cartResult = mockMvc.perform(get("/cart")).andReturn();
        String cartId = cartResult.getResponse().getCookie("cartId").getValue();

        mockMvc.perform(post("/cart/items")
                        .cookie(new Cookie("cartId", cartId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemAddRequest(productId, 1))))
                .andExpect(status().isCreated());

        String orderJson = mockMvc.perform(post("/cart/checkout").cookie(new Cookie("cartId", cartId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(orderJson).get("id").asLong();

        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.items[0].productId").value(productId));
    }

    @Test
    void gettingMissingOrderReturnsNotFound() throws Exception {
        mockMvc.perform(get("/orders/{id}", 999_999_999L))
                .andExpect(status().isNotFound());
    }
}
