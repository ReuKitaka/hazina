CREATE TABLE cash_accounts (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name           VARCHAR(150) NOT NULL,
    account_id     UUID NOT NULL REFERENCES accounts(id),
    account_number VARCHAR(50),
    bank_name      VARCHAR(150),
    currency       VARCHAR(3) NOT NULL DEFAULT 'KES',
    is_active      BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE cash_transactions (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cash_account_id       UUID NOT NULL REFERENCES cash_accounts(id),
    transaction_type      VARCHAR(10) NOT NULL,
    amount                NUMERIC(19,4) NOT NULL,
    description           VARCHAR(500) NOT NULL,
    reference             VARCHAR(100),
    counterpart_account_id UUID NOT NULL REFERENCES accounts(id),
    transaction_date      DATE NOT NULL,
    journal_entry_id      UUID NOT NULL REFERENCES journal_entries(id),
    created_by            UUID NOT NULL REFERENCES users(id),
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT ct_type_check     CHECK (transaction_type IN ('RECEIPT','PAYMENT')),
    CONSTRAINT ct_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_cash_accounts_account_id     ON cash_accounts(account_id);
CREATE INDEX idx_cash_tx_cash_account_id      ON cash_transactions(cash_account_id);
CREATE INDEX idx_cash_tx_transaction_date     ON cash_transactions(transaction_date);
CREATE INDEX idx_cash_tx_journal_entry_id     ON cash_transactions(journal_entry_id);
