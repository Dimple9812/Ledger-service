package com.Ledger_service.controller;

import com.Ledger_service.dto.request.response.AccountBalanceResponse;
import com.Ledger_service.service.JournalEntryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final JournalEntryService journalEntryService;
    public AccountController(JournalEntryService journalEntryService) {
        this.journalEntryService = journalEntryService;
    }

    @GetMapping("/{accountCode}/balance")
    public ResponseEntity<AccountBalanceResponse> getAccountBalance(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String accountCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate asOf) {

        AccountBalanceResponse response = journalEntryService.getAccountBalance(tenantId, accountCode, asOf);
        return ResponseEntity.ok(response);
    }
}