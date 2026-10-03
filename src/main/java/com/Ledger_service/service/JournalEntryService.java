package com.Ledger_service.service;

import com.Ledger_service.dto.request.JournalEntryLineRequest;
import com.Ledger_service.dto.request.JournalEntryRequest;
import com.Ledger_service.dto.request.response.AccountBalanceResponse;
import com.Ledger_service.entity.JournalEntry;
import com.Ledger_service.entity.JournalEntryLine;
import com.Ledger_service.enums.Direction;
import com.Ledger_service.exception.ApiException;
import com.Ledger_service.repository.JournalEntryLineRepository;
import com.Ledger_service.repository.JournalEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;

    public JournalEntryService(
            JournalEntryRepository journalEntryRepository,
            JournalEntryLineRepository journalEntryLineRepository) {

        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
    }

    @Transactional
    public JournalEntry createJournalEntry(String tenantId, JournalEntryRequest request) {

        if (tenantId == null || tenantId.isBlank()) {
            throw new ApiException(
                    "Tenant ID is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (request.getLines() == null || request.getLines().size() < 2) {
            throw new ApiException(
                    "At least two journal lines are required",
                    HttpStatus.BAD_REQUEST
            );
        }

        var existingEntry =
                journalEntryRepository.findByTenantIdAndIdempotencyKey(
                        tenantId,
                        request.getIdempotencyKey()
                );

        if (existingEntry.isPresent()) {
            throw new ApiException(
                    "Journal entry already exists for this idempotency key",
                    HttpStatus.CONFLICT
            );
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (JournalEntryLineRequest line : request.getLines()) {

            if (line.getAmount() == null ||
                    line.getAmount().compareTo(BigDecimal.ZERO) <= 0) {

                throw new ApiException(
                        "Amount must be greater than zero",
                        HttpStatus.BAD_REQUEST
                );
            }

            if (line.getDirection() == Direction.DEBIT) {
                totalDebit = totalDebit.add(line.getAmount());
            } else if (line.getDirection() == Direction.CREDIT) {
                totalCredit = totalCredit.add(line.getAmount());
            }
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new ApiException(
                    "Total debit must equal total credit",
                    HttpStatus.BAD_REQUEST
            );
        }

        JournalEntry entry = new JournalEntry();

        entry.setId(UUID.randomUUID());
        entry.setTenantId(tenantId);
        entry.setEntryDate(request.getEntryDate());
        entry.setDescription(request.getDescription());
        entry.setIdempotencyKey(request.getIdempotencyKey());
        entry.setPostedAt(Instant.now());

        JournalEntry savedEntry = journalEntryRepository.save(entry);
        for (JournalEntryLineRequest lineRequest : request.getLines()) {
            JournalEntryLine line = new JournalEntryLine();

            line.setId(UUID.randomUUID());
            line.setTenantId(tenantId);
            line.setEntryId(savedEntry.getId());
            line.setAccountCode(lineRequest.getAccountCode());
            line.setAmount(lineRequest.getAmount());
            line.setDirection(lineRequest.getDirection());

            journalEntryLineRepository.save(line);
        }
        return savedEntry;
    }

    @Transactional(readOnly = true)
    public Page<JournalEntry> getAllEntries(
            String tenantId,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid pagination parameters"
            );
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "From date cannot be after to date"
            );
        }
        return journalEntryRepository.findEntriesWithFilters(
                tenantId,
                from,
                to,
                PageRequest.of(page, size)
        );
    }

    @Transactional(readOnly = true)
    public JournalEntry getEntryById(String tenantId, UUID id) {
        return journalEntryRepository
                .findByTenantIdAndId(tenantId, id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Journal entry not found"
                        )
                );
    }

    @Transactional
    public JournalEntry reverseEntry(String tenantId, UUID id) {
        JournalEntry originalEntry = journalEntryRepository
                .findByTenantIdAndId(tenantId, id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Journal entry not found")
                );

        boolean alreadyReversed = journalEntryRepository
                .findByTenantIdAndReversesEntryId(tenantId, id)
                .isPresent();

        if (alreadyReversed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Journal entry has already been reversed"
            );
        }

        List<JournalEntryLine> originalLines =
                journalEntryLineRepository
                        .findByTenantIdAndEntryId(tenantId, id);

        if (originalLines.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot reverse entry without transaction lines"
            );
        }

        JournalEntry reversalEntry = new JournalEntry();
        reversalEntry.setTenantId(tenantId);
        reversalEntry.setEntryDate(LocalDate.now());
        reversalEntry.setDescription(
                "Reversal of journal entry: " + id
        );
        reversalEntry.setIdempotencyKey(
                "reversal-" + id
        );
        reversalEntry.setPostedAt(Instant.now());
        reversalEntry.setReversesEntryId(id);


        JournalEntry savedReversal =
                journalEntryRepository.save(reversalEntry);

        for (JournalEntryLine originalLine : originalLines) {
            JournalEntryLine reversalLine = new JournalEntryLine();
            reversalLine.setTenantId(tenantId);
            reversalLine.setEntryId(savedReversal.getId());
            reversalLine.setAccountCode(
                    originalLine.getAccountCode()
            );

            reversalLine.setAmount(
                    originalLine.getAmount()
            );

            Direction oppositeDirection =
                    originalLine.getDirection() == Direction.DEBIT
                            ? Direction.CREDIT
                            : Direction.DEBIT;
            reversalLine.setDirection(oppositeDirection);
            reversalLine.setId(UUID.randomUUID());
            journalEntryLineRepository.save(reversalLine);
        }
        return savedReversal;
    }

    @Transactional(readOnly = true)
    public AccountBalanceResponse getAccountBalance(
            String tenantId,
            String accountCode,
            LocalDate asOf) {

        List<JournalEntryLine> lines;

        if (asOf == null) {
            lines = journalEntryLineRepository
                    .findByTenantIdAndAccountCode(tenantId, accountCode);
        } else {
            lines = journalEntryLineRepository
                    .findByTenantIdAndAccountCodeAndJournalEntry_EntryDateLessThanEqual(
                            tenantId, accountCode, asOf);
        }

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (JournalEntryLine line : lines) {
            if (line.getDirection() == Direction.DEBIT) {
                totalDebits = totalDebits.add(line.getAmount());
            } else {
                totalCredits = totalCredits.add(line.getAmount());
            }
        }

        BigDecimal netDebitBalance = totalDebits.subtract(totalCredits);
        return new AccountBalanceResponse(
                accountCode,
                totalDebits,
                totalCredits,
                netDebitBalance
        );
    }

}


