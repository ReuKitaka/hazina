CREATE SEQUENCE bill_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE suppliers (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    supplier_code VARCHAR(20)  NOT NULL UNIQUE,
    name          VARCHAR(200) NOT NULL,
    email         VARCHAR(255),
    phone         VARCHAR(20),
    address       TEXT,
    tax_pin       VARCHAR(20),
    is_active     BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE bills (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bill_number      VARCHAR(20)  NOT NULL UNIQUE,
    supplier_id      UUID NOT NULL REFERENCES suppliers(id),
    ap_account_id    UUID NOT NULL REFERENCES accounts(id),
    bill_date        DATE NOT NULL,
    due_date         DATE NOT NULL,
    supplier_ref     VARCHAR(100),
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    total_amount     NUMERIC(19,4) NOT NULL DEFAULT 0,
    paid_amount      NUMERIC(19,4) NOT NULL DEFAULT 0,
    journal_entry_id UUID REFERENCES journal_entries(id),
    notes            TEXT,
    created_by       UUID NOT NULL REFERENCES users(id),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT bill_status_check    CHECK (status IN ('DRAFT','APPROVED','PARTIALLY_PAID','PAID','CANCELLED')),
    CONSTRAINT bill_due_after_bill  CHECK (due_date >= bill_date)
);

CREATE TABLE bill_lines (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bill_id            UUID NOT NULL REFERENCES bills(id) ON DELETE CASCADE,
    description        VARCHAR(255) NOT NULL,
    quantity           NUMERIC(10,2) NOT NULL,
    unit_price         NUMERIC(19,4) NOT NULL,
    amount             NUMERIC(19,4) NOT NULL,
    expense_account_id UUID NOT NULL REFERENCES accounts(id),
    line_order         INT  NOT NULL DEFAULT 0,

    CONSTRAINT bl_quantity_positive   CHECK (quantity   > 0),
    CONSTRAINT bl_unit_price_positive CHECK (unit_price > 0)
);

CREATE TABLE ap_payments (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bill_id            UUID NOT NULL REFERENCES bills(id),
    supplier_id        UUID NOT NULL REFERENCES suppliers(id),
    payment_date       DATE NOT NULL,
    amount_paid        NUMERIC(19,4) NOT NULL,
    payment_method     VARCHAR(20)  NOT NULL DEFAULT 'BANK_TRANSFER',
    payment_account_id UUID NOT NULL REFERENCES accounts(id),
    journal_entry_id   UUID NOT NULL REFERENCES journal_entries(id),
    notes              VARCHAR(500),
    created_by         UUID NOT NULL REFERENCES users(id),
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT ap_payment_amount_positive CHECK (amount_paid > 0),
    CONSTRAINT ap_payment_method_check    CHECK (payment_method IN ('CASH','BANK_TRANSFER','CHEQUE','MOBILE_MONEY'))
);

CREATE INDEX idx_suppliers_code          ON suppliers(supplier_code);
CREATE INDEX idx_bills_supplier_id       ON bills(supplier_id);
CREATE INDEX idx_bills_status            ON bills(status);
CREATE INDEX idx_bills_due_date          ON bills(due_date);
CREATE INDEX idx_bill_lines_bill_id      ON bill_lines(bill_id);
CREATE INDEX idx_ap_payments_bill_id     ON ap_payments(bill_id);
CREATE INDEX idx_ap_payments_supplier_id ON ap_payments(supplier_id);
