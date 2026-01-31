package com.subscriptiontracker.openapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Generates OpenAPI schema during the test phase.
 * The schema is saved to backend/docs/api-schema.json for AI agent consumption.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiSchemaGeneratorTest {

    private static final String OUTPUT_DIR = "docs";
    private static final String OUTPUT_FILE = "api-schema.json";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generateOpenApiSchema() throws Exception {
        String schema = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Path outputDir = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outputDir);

        Path outputPath = outputDir.resolve(OUTPUT_FILE);
        Files.writeString(outputPath, schema);

        System.out.println("OpenAPI schema generated: " + outputPath.toAbsolutePath());
    }
}
