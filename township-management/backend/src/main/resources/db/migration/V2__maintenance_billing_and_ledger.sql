-- ==============================================================================
-- V2__maintenance_billing_and_ledger.sql
-- Phase 5: Maintenance Billing, Config, Flat Ledger & Payment Records
-- All financial amounts stored strictly as NUMERIC (never floating point)
-- ==============================================================================

-- 1. Township Billing Configurations
CREATE TABLE IF NOT EXISTS billing_configurations (
    id BIGSERIAL PRIMARY KEY,
    base_rate_per_sq_ft NUMERIC(10, 2) NOT NULL DEFAULT 6.50,
    grace_period_days INTEGER NOT NULL DEFAULT 15,
    late_fee_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE',
    late_fee_percentage NUMERIC(5, 2) NOT NULL DEFAULT 10.00,
    late_fee_fixed_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    sinking_fund_per_flat NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Maintenance Bills (Monthly invoices per flat)
CREATE TABLE IF NOT EXISTS maintenance_bills (
    id BIGSERIAL PRIMARY KEY,
    flat_id BIGINT NOT NULL REFERENCES flats(id) ON DELETE RESTRICT,
    billing_month VARCHAR(7) NOT NULL, -- Format: YYYY-MM (e.g. '2026-10')
    flat_area_sq_ft NUMERIC(10, 2) NOT NULL,
    rate_per_sq_ft NUMERIC(10, 2) NOT NULL,
    base_amount NUMERIC(12, 2) NOT NULL,
    late_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_due NUMERIC(12, 2) NOT NULL,
    paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    outstanding_balance NUMERIC(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL, -- UNPAID, PARTIALLY_PAID, PAID, OVERDUE
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_flat_billing_month UNIQUE (flat_id, billing_month)
);

-- 3. Flat Ledger Entries (Full Financial Audit Trail)
CREATE TABLE IF NOT EXISTS flat_ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    flat_id BIGINT NOT NULL REFERENCES flats(id) ON DELETE RESTRICT,
    bill_id BIGINT REFERENCES maintenance_bills(id) ON DELETE SET NULL,
    entry_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    entry_type VARCHAR(40) NOT NULL, -- BILL_GENERATION, PAYMENT, LATE_FEE_ASSESSED, ADJUSTMENT
    description VARCHAR(255) NOT NULL,
    debit NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    credit NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    running_balance NUMERIC(12, 2) NOT NULL,
    transaction_reference VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Payment Records
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    bill_id BIGINT NOT NULL REFERENCES maintenance_bills(id) ON DELETE RESTRICT,
    flat_id BIGINT NOT NULL REFERENCES flats(id) ON DELETE RESTRICT,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    amount NUMERIC(12, 2) NOT NULL,
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_method VARCHAR(50) NOT NULL,
    reference_number VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    notes VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_bills_flat_id ON maintenance_bills(flat_id);
CREATE INDEX IF NOT EXISTS idx_bills_billing_month ON maintenance_bills(billing_month);
CREATE INDEX IF NOT EXISTS idx_bills_status ON maintenance_bills(status);
CREATE INDEX IF NOT EXISTS idx_ledger_flat_id ON flat_ledger_entries(flat_id);
CREATE INDEX IF NOT EXISTS idx_ledger_entry_date ON flat_ledger_entries(entry_date);
CREATE INDEX IF NOT EXISTS idx_payments_bill_id ON payments(bill_id);
CREATE INDEX IF NOT EXISTS idx_payments_flat_id ON payments(flat_id);

