package com.Ledger_service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

    @SpringBootTest
    @AutoConfigureMockMvc
    class JournalEntryTenantIsolationTest {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void tenantBCannotAccessTenantAEntry() throws Exception {
            String uniqueKey = "isolation-test-" + UUID.randomUUID();


            String requestBody = """
                {
                  "entryDate": "2026-10-03",
                  "description": "Tenant isolation test",
                  "idempotencyKey": "%s",
                  "lines": [
                    {
                      "accountCode": "CASH",
                      "amount": 100,
                      "direction": "DEBIT"
                    },
                    {
                      "accountCode": "REVENUE",
                      "amount": 100,
                      "direction": "CREDIT"
                    }
                  ]
                }
                """.formatted(uniqueKey);

            // Tenant A creates an entry
            String response = mockMvc.perform(post("/api/v1/journal-entries")
                            .header("X-Tenant-Id", "tenant-isolation-A")
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String entryId = com.jayway.jsonpath.JsonPath.read(response, "$.id");

            // Tenant A can read its own entry
            mockMvc.perform(get("/api/v1/journal-entries/" + entryId)
                            .header("X-Tenant-Id", "tenant-isolation-A"))
                    .andExpect(status().isOk());

            // Tenant B cannot read Tenant A's entry
            mockMvc.perform(get("/api/v1/journal-entries/" + entryId)
                            .header("X-Tenant-Id", "tenant-isolation-B"))
                    .andExpect(status().isNotFound());

            // Tenant B cannot reverse Tenant A's entry
            mockMvc.perform(post("/api/v1/journal-entries/" + entryId + "/reverse")
                            .header("X-Tenant-Id", "tenant-isolation-B"))
                    .andExpect(status().isNotFound());

            // Tenant B has no balance from Tenant A's entry
            mockMvc.perform(get("/api/v1/accounts/CASH/balance")
                            .header("X-Tenant-Id", "tenant-isolation-B"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDebits").value(0));
        }
    }
