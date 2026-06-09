CREATE TABLE customer_transaction_details (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    customer_name VARCHAR(150) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    transaction_reference VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    transaction_amount NUMERIC(19, 2) NOT NULL,
    current_balance NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_customer_transaction_account_number UNIQUE (account_number),
    CONSTRAINT uk_customer_transaction_reference UNIQUE (transaction_reference),
    CONSTRAINT uk_customer_transaction_idempotency_key UNIQUE (idempotency_key)
);
