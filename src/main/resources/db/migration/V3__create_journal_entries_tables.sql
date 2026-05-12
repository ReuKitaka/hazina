CREATE SEQUENCE journal_entry_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE journal_entries (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entry_number VARCHAR(20)  NOT NULL UNIQUE,
    entry_date   DATE         NOT NULL,
    description  VARCHAR(500) NOT NULL,
    reference    VARCHAR(100),
    status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    posted_by    UUID REFERENCES users(id),
    posted_at    TIMESTAMP,
    created_by   UUID NOT NULL REFERENCES users(id),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT je_status_check CHECK (status IN ('DRAFT','POSTED','REVERSED'))
);

CREATE TABLE journal_entry_lines (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    account_id       UUID NOT NULL REFERENCES accounts(id),
    description      VARCHAR(255),
    debit_amount     NUMERIC(19,4) NOT NULL DEFAULT 0,
    credit_amount    NUMERIC(19,4) NOT NULL DEFAULT 0,
    line_order       INT  NOT NULL DEFAULT 0,

    CONSTRAINT jel_amounts_check CHECK (
        (debit_amount > 0 AND credit_amount = 0) OR
        (credit_amount > 0 AND debit_amount = 0)
    )
);

CREATE INDEX idx_je_entry_date  ON journal_entries(entry_date);
CREATE INDEX idx_je_status      ON journal_entries(status);
CREATE INDEX idx_je_created_by  ON journal_entries(created_by);
CREATE INDEX idx_jel_entry_id   ON journal_entry_lines(journal_entry_id);
CREATE INDEX idx_jel_account_id ON journal_entry_lines(account_id);
