package dev.efrain.gilacommerce.controller;

import dev.efrain.gilacommerce.dto.ProductCreateRequest;
import dev.efrain.gilacommerce.dto.ProductUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String uniqueSku(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void createGetUpdateAndDeleteProduct() throws Exception {
        String sku = uniqueSku("CTRL");
        ProductCreateRequest createRequest = new ProductCreateRequest(
                sku, "Test Product", "A description", "Category", new BigDecimal("19.99"), 5, new BigDecimal("1.2"));

        String createResponse = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value(sku))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"));

        ProductUpdateRequest updateRequest = new ProductUpdateRequest(
                "Updated Name", "Updated description", "Category", new BigDecimal("29.99"), 3, null);
        mockMvc.perform(put("/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.sku").value(sku));

        mockMvc.perform(delete("/products/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingDuplicateSkuReturnsConflict() throws Exception {
        String sku = uniqueSku("DUP");
        ProductCreateRequest createRequest = new ProductCreateRequest(
                sku, "First", null, null, new BigDecimal("10.00"), 1, null);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void gettingMissingProductReturnsNotFound() throws Exception {
        mockMvc.perform(get("/products/{id}", 999_999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingWithInvalidPayloadReturnsBadRequest() throws Exception {
        String invalidJson = """
                {"sku": "", "name": "", "price": -1, "stock": -1}
                """;

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    void searchReturnsPagedResults() throws Exception {
        String sku = uniqueSku("SEARCH");
        ProductCreateRequest createRequest = new ProductCreateRequest(
                sku, "SearchableWidgetXyz", null, null, new BigDecimal("5.00"), 1, null);
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/products").param("name", "searchablewidgetxyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value(sku))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void importingCsvReturnsCreatedWithLocationHeader() throws Exception {
        String sku = uniqueSku("IMPORT");
        String csv = String.join("\n",
                "name,sku,description,category,price,stock,weight_kg",
                "Imported Widget," + sku + ",desc,Category,1.00,1,0.1");
        MockMultipartFile file = new MockMultipartFile(
                "file", "import.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/products/import").file(file))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.job.successCount").value(1))
                .andExpect(jsonPath("$.job.failureCount").value(0));
    }
}
