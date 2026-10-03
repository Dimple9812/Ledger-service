package com.Ledger_service.dto.request.response;

import java.math.BigDecimal;

    public record AccountBalanceResponse(
            String accountCode,
            BigDecimal totalDebits,
            BigDecimal totalCredits,
            BigDecimal netDebitBalance
    ) {
    }

