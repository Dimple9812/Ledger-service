package com.Ledger_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

    @SpringBootTest
    @AutoConfigureMockMvc
    class JournalEntryValidationAndIdempotencyTest {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void unbalancedEntryShouldBeRejected() throws Exception {

            String requestBody = """
                {
                  "entryDate": "2026-10-04",
                  "description": "Unbalanced transaction test",
                  "idempotencyKey": "unbalanced-test-%s",
                  "lines": [
                    {
                      "accountCode": "CASH",
                      "amount": 500,
                      "direction": "DEBIT"
                    },
                    {
                      "accountCode": "REVENUE",
                      "amount": 400,
                      "direction": "CREDIT"
                    }
                  ]
                }
                """.formatted(UUID.randomUUID());

            mockMvc.perform(post("/api/v1/journal-entries")
                            .header("X-Tenant-Id", "test-unbalanced")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void duplicateRequestShouldCreateOnlyOneEntry() throws Exception {

            String tenantId = "test-idempotency-" + UUID.randomUUID();
            String uniqueKey = "payment-" + UUID.randomUUID();

            String requestBody = """
                {
                  "entryDate": "2026-10-04",
                  "description": "Duplicate request test",
                  "idempotencyKey": "%s",
                  "lines": [
                    {
                      "accountCode": "CASH",
                      "amount": 1000,
                      "direction": "DEBIT"
                    },
                    {
                      "accountCode": "REVENUE",
                      "amount": 1000,
                      "direction": "CREDIT"
                    }
                  ]
                }
                """.formatted(uniqueKey);

            // First request
            mockMvc.perform(post("/api/v1/journal-entries")
                            .header("X-Tenant-Id", tenantId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated());

            // Same request sent again
            mockMvc.perform(post("/api/v1/journal-entries")
                            .header("X-Tenant-Id", tenantId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isConflict());

            // Verify only one entry exists for this tenant
            mockMvc.perform(get("/api/v1/journal-entries")
                            .header("X-Tenant-Id", tenantId)
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }
