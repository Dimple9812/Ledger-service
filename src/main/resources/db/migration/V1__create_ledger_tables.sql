
CREATE TABLE journal_entries (
                                 id UUID PRIMARY KEY,
                                 tenant_id VARCHAR(100) NOT NULL,
                                 entry_date DATE NOT NULL,
                                 description VARCHAR(500) NOT NULL,
                                 idempotency_key VARCHAR(255) NOT NULL,
                                 posted_at TIMESTAMPTZ NOT NULL,
                                 reverses_entry_id UUID,
                                 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT uq_journal_tenant_id
                                     UNIQUE (tenant_id, id),

                                 CONSTRAINT uq_journal_idempotency
                                     UNIQUE (tenant_id, idempotency_key),

                                 CONSTRAINT uq_journal_reversal
                                     UNIQUE (tenant_id, reverses_entry_id),

                                 CONSTRAINT fk_journal_reversal
                                     FOREIGN KEY (tenant_id, reverses_entry_id)
                                         REFERENCES journal_entries (tenant_id, id)
);

CREATE TABLE journal_entry_lines (
                                     id UUID PRIMARY KEY,
                                     tenant_id VARCHAR(100) NOT NULL,
                                     entry_id UUID NOT NULL,
                                     account_code VARCHAR(100) NOT NULL,
                                     amount NUMERIC(19, 2) NOT NULL,
                                     direction VARCHAR(10) NOT NULL,

                                     CONSTRAINT chk_line_amount
                                         CHECK (amount > 0),

                                     CONSTRAINT chk_line_direction
                                         CHECK (direction IN ('DEBIT', 'CREDIT')),

                                     CONSTRAINT fk_line_entry
                                         FOREIGN KEY (tenant_id, entry_id)
                                             REFERENCES journal_entries (tenant_id, id)
);

CREATE TABLE webhook_events (
                                id UUID PRIMARY KEY,
                                tenant_id VARCHAR(100) NOT NULL,
                                event_id VARCHAR(255) NOT NULL,
                                status VARCHAR(20) NOT NULL,
                                payload TEXT NOT NULL,
                                retry_count INTEGER NOT NULL DEFAULT 0,
                                last_error TEXT,
                                journal_entry_id UUID,
                                received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                processed_at TIMESTAMPTZ,

                                CONSTRAINT uq_webhook_tenant_event
                                    UNIQUE (tenant_id, event_id),

                                CONSTRAINT chk_webhook_retry
                                    CHECK (retry_count >= 0),

                                CONSTRAINT chk_webhook_status
                                    CHECK (status IN ('RECEIVED', 'PROCESSING', 'PROCESSED', 'FAILED')),

                                CONSTRAINT fk_webhook_journal
                                    FOREIGN KEY (tenant_id, journal_entry_id)
                                        REFERENCES journal_entries (tenant_id, id)
);

CREATE INDEX idx_journal_tenant_date
    ON journal_entries (tenant_id, entry_date);

CREATE INDEX idx_lines_tenant_account
    ON journal_entry_lines (tenant_id, account_code);

CREATE INDEX idx_lines_tenant_entry
    ON journal_entry_lines (tenant_id, entry_id);

CREATE INDEX idx_webhook_tenant_status
    ON webhook_events (tenant_id, status, received_at);
