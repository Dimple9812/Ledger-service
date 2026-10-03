package com.Ledger_service.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

    @Entity
    @Table(name = "journal_entries")
    public class JournalEntry {

        @Id
        private UUID id;

        @Column(name = "tenant_id", nullable = false, length = 100)
        private String tenantId;

        @Column(name = "entry_date", nullable = false)
        private LocalDate entryDate;

        @Column(name = "description", nullable = false, length = 500)
        private String description;

        @Column(name = "idempotency_key", nullable = false)
        private String idempotencyKey;

        @Column(name = "posted_at", nullable = false)
        private Instant postedAt;

        @Column(name = "reverses_entry_id")
        private UUID reversesEntryId;

        @Column(name = "created_at", nullable = false)
        private Instant createdAt;

        public JournalEntry() {
        }

        @PrePersist
        protected void onCreate() {
            if (id == null) {
                id = UUID.randomUUID();
            }
            if (createdAt == null) {
                createdAt = Instant.now();
            }
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

        public Instant getPostedAt() {
            return postedAt;
        }

        public void setPostedAt(Instant postedAt) {
            this.postedAt = postedAt;
        }

        public UUID getReversesEntryId() {
            return reversesEntryId;
        }

        public void setReversesEntryId(UUID reversesEntryId) {
            this.reversesEntryId = reversesEntryId;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }

