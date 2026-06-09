CREATE TABLE fund_transfers (
    id BIGSERIAL PRIMARY KEY,
    transfer_reference VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    source_account_number VARCHAR(50) NOT NULL,
    destination_account_number VARCHAR(50) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    source_balance_after_transfer NUMERIC(19, 2) NOT NULL,
    destination_balance_after_transfer NUMERIC(19, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_fund_transfer_reference UNIQUE (transfer_reference),
    CONSTRAINT uk_fund_transfer_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_fund_transfers_source_account ON fund_transfers (source_account_number);
CREATE INDEX idx_fund_transfers_destination_account ON fund_transfers (destination_account_number);
