-- ==============================================================================
-- V5__visitor_management_and_gate_security.sql
-- Schema for Phase 8: Visitor Management, Pre-Approvals, Gate Security, & Offline Sync
-- ==============================================================================

-- Update users role check constraint to include GUARD
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('RESIDENT', 'HELP_DESK', 'GUARD'));

CREATE TABLE IF NOT EXISTS visitors (
    id BIGSERIAL PRIMARY KEY,
    pass_code VARCHAR(30) NOT NULL UNIQUE,
    pass_token VARCHAR(100) NOT NULL UNIQUE,
    flat_id BIGINT NOT NULL REFERENCES flats(id) ON DELETE RESTRICT,
    resident_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    visitor_name VARCHAR(100) NOT NULL,
    visitor_phone VARCHAR(25) NOT NULL,
    visitor_type VARCHAR(30) NOT NULL,
    purpose VARCHAR(255),
    vehicle_number VARCHAR(50),
    status VARCHAR(30) NOT NULL,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    is_one_time BOOLEAN NOT NULL DEFAULT TRUE,
    check_in_time TIMESTAMP WITH TIME ZONE,
    check_out_time TIMESTAMP WITH TIME ZONE,
    entry_guard_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    exit_guard_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    entry_gate VARCHAR(50) DEFAULT 'Gate 1 (Main)',
    exit_gate VARCHAR(50),
    photo_storage_path VARCHAR(255),
    photo_original_filename VARCHAR(255),
    photo_size_bytes BIGINT,
    photo_captured_at TIMESTAMP WITH TIME ZONE,
    photo_deleted_at TIMESTAMP WITH TIME ZONE,
    approved_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    approved_at TIMESTAMP WITH TIME ZONE,
    rejection_reason VARCHAR(255),
    offline_sync_id VARCHAR(100) UNIQUE,
    is_offline_sync BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS visitor_activities (
    id BIGSERIAL PRIMARY KEY,
    visitor_id BIGINT NOT NULL REFERENCES visitors(id) ON DELETE CASCADE,
    action_type VARCHAR(50) NOT NULL,
    performed_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    details TEXT,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Performance indices
CREATE INDEX IF NOT EXISTS idx_visitors_flat ON visitors(flat_id);
CREATE INDEX IF NOT EXISTS idx_visitors_resident ON visitors(resident_id);
CREATE INDEX IF NOT EXISTS idx_visitors_pass_code ON visitors(pass_code);
CREATE INDEX IF NOT EXISTS idx_visitors_pass_token ON visitors(pass_token);
CREATE INDEX IF NOT EXISTS idx_visitors_status ON visitors(status);
CREATE INDEX IF NOT EXISTS idx_visitors_phone ON visitors(visitor_phone);
CREATE INDEX IF NOT EXISTS idx_visitors_validity ON visitors(valid_from, valid_until);
CREATE INDEX IF NOT EXISTS idx_visitors_photo_retention ON visitors(photo_captured_at, photo_deleted_at);
CREATE INDEX IF NOT EXISTS idx_visitor_activities_visitor ON visitor_activities(visitor_id, timestamp);
