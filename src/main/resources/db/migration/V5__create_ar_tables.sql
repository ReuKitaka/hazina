CREATE SEQUENCE invoice_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE customers (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_code VARCHAR(20)  NOT NULL UNIQUE,
    name          VARCHAR(200) NOT NULL,
    email         VARCHAR(255),
    phone         VARCHAR(20),
    address       TEXT,
    credit_limit  NUMERIC(19,4),
    is_active     BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE invoices (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_number VARCHAR(20)  NOT NULL UNIQUE,
    customer_id    UUID NOT NULL REFERENCES customers(id),
    ar_account_id  UUID NOT NULL REFERENCES accounts(id),
    issue_date     DATE NOT NULL,
    due_date       DATE NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    total_amount   NUMERIC(19,4) NOT NULL DEFAULT 0,
    paid_amount    NUMERIC(19,4) NOT NULL DEFAULT 0,
    journal_entry_id UUID REFERENCES journal_entries(id),
    notes          TEXT,
    created_by     UUID NOT NULL REFERENCES users(id),
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT inv_status_check CHECK (status IN ('DRAFT','APPROVED','PARTIALLY_PAID','PAID','CANCELLED')),
    CONSTRAINT inv_due_after_issue CHECK (due_date >= issue_date)
);

CREATE TABLE invoice_lines (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_id         UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description        VARCHAR(255) NOT NULL,
    quantity           NUMERIC(10,2) NOT NULL,
    unit_price         NUMERIC(19,4) NOT NULL,
    amount             NUMERIC(19,4) NOT NULL,
    revenue_account_id UUID NOT NULL REFERENCES accounts(id),
    line_order         INT  NOT NULL DEFAULT 0,

    CONSTRAINT il_quantity_positive   CHECK (quantity   > 0),
    CONSTRAINT il_unit_price_positive CHECK (unit_price > 0)
);

CREATE TABLE ar_receipts (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_id         UUID NOT NULL REFERENCES invoices(id),
    customer_id        UUID NOT NULL REFERENCES customers(id),
    receipt_date       DATE NOT NULL,
    amount_received    NUMERIC(19,4) NOT NULL,
    payment_method     VARCHAR(20)  NOT NULL DEFAULT 'BANK_TRANSFER',
    payment_account_id UUID NOT NULL REFERENCES accounts(id),
    journal_entry_id   UUID NOT NULL REFERENCES journal_entries(id),
    notes              VARCHAR(500),
    created_by         UUID NOT NULL REFERENCES users(id),
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT ar_receipt_amount_positive CHECK (amount_received > 0),
    CONSTRAINT ar_receipt_method_check    CHECK (payment_method IN ('CASH','BANK_TRANSFER','CHEQUE','MOBILE_MONEY'))
);

CREATE INDEX idx_customers_code          ON customers(customer_code);
CREATE INDEX idx_invoices_customer_id    ON invoices(customer_id);
CREATE INDEX idx_invoices_status         ON invoices(status);
CREATE INDEX idx_invoices_due_date       ON invoices(due_date);
CREATE INDEX idx_invoice_lines_inv_id    ON invoice_lines(invoice_id);
CREATE INDEX idx_ar_receipts_invoice_id  ON ar_receipts(invoice_id);
CREATE INDEX idx_ar_receipts_customer_id ON ar_receipts(customer_id);
