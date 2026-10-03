package com.Ledger_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

    public class JournalEntryRequest {

        @NotNull(message = "Entry date is required")
        private LocalDate entryDate;

        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description cannot exceed 500 characters")
        private String description;

        @NotBlank(message = "Idempotency key is required")
        private String idempotencyKey;

        @NotEmpty(message = "At least one journal line is required")
        @Valid
        private List<JournalEntryLineRequest> lines;

        public JournalEntryRequest() {
        }

        public LocalDate getEntryDate() {
            return entryDate;
        }

        public void setEntryDate(LocalDate entryDate) {
            this.entryDate = entryDate;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }

        public List<JournalEntryLineRequest> getLines() {
            return lines;
        }

        public void setLines(List<JournalEntryLineRequest> lines) {
            this.lines = lines;
        }
    }

