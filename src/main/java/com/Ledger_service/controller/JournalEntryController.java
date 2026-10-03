package com.Ledger_service.controller;

import com.Ledger_service.dto.request.JournalEntryRequest;
import com.Ledger_service.entity.JournalEntry;
import com.Ledger_service.service.JournalEntryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
    @RequestMapping("/api/v1/journal-entries")
    public class JournalEntryController {

        private final JournalEntryService journalEntryService;

        public JournalEntryController(
                JournalEntryService journalEntryService) {
            this.journalEntryService = journalEntryService;
        }

        @PostMapping
        public ResponseEntity<JournalEntry> createJournalEntry(
                @RequestHeader("X-Tenant-Id") String tenantId,
                @Valid @RequestBody JournalEntryRequest request) {

            JournalEntry createdEntry = journalEntryService.createJournalEntry(tenantId, request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(createdEntry);
        }
    @GetMapping
    public ResponseEntity<Page<JournalEntry>> getAllEntries(

            @RequestHeader("X-Tenant-Id")
            String tenantId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size) {

        return ResponseEntity.ok(
                journalEntryService.getAllEntries(
                        tenantId,
                        from,
                        to,
                        page,
                        size
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<JournalEntry> getEntryById(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(journalEntryService.getEntryById(tenantId, id)
        );
    }
    @PostMapping("/{id}/reverse")
    public ResponseEntity<JournalEntry> reverseEntry(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id) {
        JournalEntry reversalEntry = journalEntryService.reverseEntry(tenantId, id);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reversalEntry);
    }

}

