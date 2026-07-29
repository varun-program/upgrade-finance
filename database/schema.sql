-- Upgrade Finance Database Schema

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bank_accounts (
    id VARCHAR(100) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bank_name VARCHAR(100) NOT NULL,
    account_number_suffix VARCHAR(10) NOT NULL,
    balance DECIMAL(15, 2) DEFAULT 0.00,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(100) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DECIMAL(15, 2) NOT NULL,
    timestamp BIGINT NOT NULL,
    merchant VARCHAR(255),
    upi_id VARCHAR(255),
    reference_number VARCHAR(100) UNIQUE,
    bank VARCHAR(100),
    transaction_type VARCHAR(10) CHECK (transaction_type IN ('DEBIT', 'CREDIT')),
    category VARCHAR(100),
    is_deleted BOOLEAN DEFAULT FALSE,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS budgets (
    id VARCHAR(100) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(100) NOT NULL,
    limit_amount DECIMAL(15, 2) NOT NULL,
    period VARCHAR(20) DEFAULT 'MONTHLY',
    is_deleted BOOLEAN DEFAULT FALSE,
    updated_at BIGINT NOT NULL,
    UNIQUE (user_id, category)
);

CREATE TABLE IF NOT EXISTS savings_goals (
    id VARCHAR(100) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    target_amount DECIMAL(15, 2) NOT NULL,
    saved_amount DECIMAL(15, 2) DEFAULT 0.00,
    target_date BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS smart_rules (
    id VARCHAR(100) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    pattern VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    updated_at BIGINT NOT NULL
);

-- Indexes for performance and quick deduplication/sync
CREATE INDEX IF NOT EXISTS idx_transactions_user_ref ON transactions(user_id, reference_number);
CREATE INDEX IF NOT EXISTS idx_transactions_user_updated ON transactions(user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_budgets_user ON budgets(user_id);
CREATE INDEX IF NOT EXISTS idx_savings_goals_user ON savings_goals(user_id);
CREATE INDEX IF NOT EXISTS idx_smart_rules_user ON smart_rules(user_id);
