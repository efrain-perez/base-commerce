package dev.efrain.gilacommerce.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ImportJobControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listAndGetImportJobDetailIncludingErrors() throws Exception {
        String badSku = "JOB-" + UUID.randomUUID().toString().substring(0, 8);
        String csv = String.join("\n",
                "name,sku,description,category,price,stock,weight_kg",
                "Broken Row," + badSku + ",desc,Category,1.00,not-a-number,0.1");
        MockMultipartFile file = new MockMultipartFile(
                "file", "job.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        String importResponse = mockMvc.perform(multipart("/products/import").file(file))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long jobId = objectMapper.readTree(importResponse).get("job").get("id").asLong();

        mockMvc.perform(get("/import-jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/import-jobs/{id}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job.id").value(jobId))
                .andExpect(jsonPath("$.errors[0].errorReason").value("stock must be a valid integer"));
    }

    @Test
    void gettingMissingImportJobReturnsNotFound() throws Exception {
        mockMvc.perform(get("/import-jobs/{id}", 999_999_999L))
                .andExpect(status().isNotFound());
    }
}
