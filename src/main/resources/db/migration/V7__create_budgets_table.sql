CREATE TABLE budgets (
    id             UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    name           VARCHAR(150) NOT NULL,
    account_id     UUID         NOT NULL REFERENCES accounts(id),
    period_start   DATE         NOT NULL,
    period_end     DATE         NOT NULL,
    budget_amount  NUMERIC(19,4) NOT NULL CHECK (budget_amount > 0),
    notes          TEXT,
    is_active      BOOLEAN      NOT NULL DEFAULT true,
    created_by     UUID         NOT NULL REFERENCES users(id),
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT budget_period_check CHECK (period_end >= period_start)
);

CREATE INDEX idx_budgets_account_period ON budgets(account_id, period_start, period_end);
