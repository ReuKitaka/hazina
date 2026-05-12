CREATE TABLE accounts (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code          VARCHAR(20)  NOT NULL UNIQUE,
    name          VARCHAR(150) NOT NULL,
    type          VARCHAR(20)  NOT NULL,
    normal_balance VARCHAR(10) NOT NULL,
    parent_id     UUID REFERENCES accounts(id),
    description   TEXT,
    is_active     BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT accounts_type_check
        CHECK (type IN ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE')),
    CONSTRAINT accounts_normal_balance_check
        CHECK (normal_balance IN ('DEBIT','CREDIT'))
);

CREATE INDEX idx_accounts_code      ON accounts(code);
CREATE INDEX idx_accounts_type      ON accounts(type);
CREATE INDEX idx_accounts_parent_id ON accounts(parent_id);
