package com.Ledger_service.repository;

import com.Ledger_service.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository
        extends JpaRepository<JournalEntry, UUID> {

    Optional<JournalEntry> findByTenantIdAndId(
            String tenantId,
            UUID id
    );

    Optional<JournalEntry> findByTenantIdAndIdempotencyKey(
            String tenantId,
            String idempotencyKey
    );

    List<JournalEntry> findAllByTenantIdOrderByEntryDateDesc(
            String tenantId
    );
    Optional<JournalEntry> findByTenantIdAndReversesEntryId(
            String tenantId,
            UUID reversesEntryId
    );

    @Query("""
        SELECT j FROM JournalEntry j
        WHERE j.tenantId = :tenantId
        AND (:fromDate IS NULL OR j.entryDate >= :fromDate)
        AND (:toDate IS NULL OR j.entryDate <= :toDate)
        ORDER BY j.entryDate DESC
        """)
    Page<JournalEntry> findEntriesWithFilters(
            @Param("tenantId") String tenantId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );
}

