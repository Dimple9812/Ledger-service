package com.Ledger_service.entity;

import com.Ledger_service.enums.Direction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

    @Entity
    @Table(name = "journal_entry_lines")
    public class JournalEntryLine {

        @Id
        private UUID id;

        @Column(name = "tenant_id", nullable = false, length = 100)
        private String tenantId;

        @Column(name = "entry_id", nullable = false)
        private UUID entryId;

        @Column(name = "account_code", nullable = false, length = 100)
        private String accountCode;

        @Column(name = "amount", nullable = false, precision = 19, scale = 2)
        private BigDecimal amount;

        @Enumerated(EnumType.STRING)
        @Column(name = "direction", nullable = false, length = 10)
        private Direction direction;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumns({
                @JoinColumn(
                        name = "tenant_id",
                        referencedColumnName = "tenant_id",
                        insertable = false,
                        updatable = false
                ),
                @JoinColumn(
                        name = "entry_id",
                        referencedColumnName = "id",
                        insertable = false,
                        updatable = false
                )
        })
        private JournalEntry journalEntry;

        public JournalEntryLine() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public UUID getEntryId() {
            return entryId;
        }

        public void setEntryId(UUID entryId) {
            this.entryId = entryId;
        }

        public String getAccountCode() {
            return accountCode;
        }

        public void setAccountCode(String accountCode) {
            this.accountCode = accountCode;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public Direction getDirection() {
            return direction;
        }

        public void setDirection(Direction direction) {
            this.direction = direction;
        }

        public JournalEntry getJournalEntry() {
            return journalEntry;
        }

        public void setJournalEntry(JournalEntry journalEntry) {
            this.journalEntry = journalEntry;
        }
    }
