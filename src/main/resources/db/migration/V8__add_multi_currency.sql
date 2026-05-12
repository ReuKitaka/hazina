CREATE TABLE exchange_rates (
    id               UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    base_currency    VARCHAR(3)   NOT NULL,
    quote_currency   VARCHAR(3)   NOT NULL,
    rate             NUMERIC(19,6) NOT NULL CHECK (rate > 0),
    effective_date   DATE         NOT NULL,
    created_by       UUID         NOT NULL REFERENCES users(id),
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rate_pair_date UNIQUE (base_currency, quote_currency, effective_date)
);

CREATE INDEX idx_exchange_rates_pair_date ON exchange_rates(base_currency, quote_currency, effective_date DESC);

ALTER TABLE journal_entry_lines
    ADD COLUMN foreign_currency VARCHAR(3),
    ADD COLUMN foreign_amount   NUMERIC(19,4),
    ADD COLUMN exchange_rate    NUMERIC(19,6);
