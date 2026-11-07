-- ==============================================================================
-- V3__payments_and_bank_reconciliation.sql
-- Phase 6: Mock Payment Gateway, Receipt Numbering & Bank CSV Statement Reconciliation
-- ==============================================================================

-- 1. Ensure payments table has test mode flag and receipt number
ALTER TABLE payments ADD COLUMN IF NOT EXISTS is_test_mode BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS receipt_number VARCHAR(100);

-- 2. Bank Statement Batches (Import Sessions)
CREATE TABLE IF NOT EXISTS bank_statement_batches (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(100) NOT NULL UNIQUE,
    filename VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    total_rows INTEGER NOT NULL DEFAULT 0,
    total_credit_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    exact_matches_count INTEGER NOT NULL DEFAULT 0,
    manual_review_count INTEGER NOT NULL DEFAULT 0,
    unmatched_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PROCESSED',
    uploaded_by VARCHAR(150),
    imported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Imported Bank Transactions
CREATE TABLE IF NOT EXISTS bank_transactions (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES bank_statement_batches(id) ON DELETE CASCADE,
    transaction_date DATE NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    utr_reference VARCHAR(100) NOT NULL UNIQUE,
    narration VARCHAR(500) NOT NULL,
    upi_id VARCHAR(150),
    match_status VARCHAR(40) NOT NULL, -- EXACT_MATCH, MANUAL_REVIEW, UNMATCHED, RECONCILED, REJECTED
    matched_flat_id BIGINT REFERENCES flats(id) ON DELETE SET NULL,
    matched_bill_id BIGINT REFERENCES maintenance_bills(id) ON DELETE SET NULL,
    confidence_score INTEGER NOT NULL DEFAULT 0,
    match_reason VARCHAR(255),
    reconciled_at TIMESTAMP,
    reconciled_by VARCHAR(150),
    payment_id BIGINT REFERENCES payments(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for Fast Search & Filtering
CREATE INDEX IF NOT EXISTS idx_bank_txns_batch_id ON bank_transactions(batch_id);
CREATE INDEX IF NOT EXISTS idx_bank_txns_match_status ON bank_transactions(match_status);
CREATE INDEX IF NOT EXISTS idx_bank_txns_utr ON bank_transactions(utr_reference);
CREATE INDEX IF NOT EXISTS idx_bank_txns_matched_flat ON bank_transactions(matched_flat_id);
CREATE INDEX IF NOT EXISTS idx_bank_txns_matched_bill ON bank_transactions(matched_bill_id);

