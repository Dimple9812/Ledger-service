package com.Ledger_service.repository;

import com.Ledger_service.entity.JournalEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

    public interface JournalEntryLineRepository
            extends JpaRepository<JournalEntryLine, UUID> {

        List<JournalEntryLine> findByTenantIdAndEntryId(
                String tenantId,
                UUID entryId
        );

        List<JournalEntryLine> findByTenantIdAndAccountCode(
                String tenantId,
                String accountCode
        );
        List<JournalEntryLine>
        findByTenantIdAndAccountCodeAndJournalEntry_EntryDateLessThanEqual(
                String tenantId,
                String accountCode,
                LocalDate asOf
        );
    }

