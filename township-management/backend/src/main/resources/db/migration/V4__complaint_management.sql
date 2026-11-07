-- =========================================================================
-- Phase 7: AI-Powered Complaint Management & Safety Escalation Migration
-- =========================================================================

CREATE TABLE complaints (
    id BIGSERIAL PRIMARY KEY,
    complaint_number VARCHAR(50) NOT NULL UNIQUE,
    flat_id BIGINT NOT NULL REFERENCES flats(id) ON DELETE RESTRICT,
    resident_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    is_critical_safety_escalation BOOLEAN NOT NULL DEFAULT FALSE,
    safety_trigger_keyword VARCHAR(100),
    ai_suggested_category VARCHAR(50),
    ai_suggested_priority VARCHAR(50),
    ai_confidence_score INT,
    assigned_team VARCHAR(100),
    assigned_staff_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    photo_storage_path VARCHAR(500),
    photo_original_filename VARCHAR(255),
    photo_content_type VARCHAR(100),
    photo_size_bytes BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response_deadline TIMESTAMP,
    resolution_deadline TIMESTAMP,
    acknowledged_at TIMESTAMP,
    resolved_at TIMESTAMP,
    closed_at TIMESTAMP,
    resolution_notes TEXT,
    resident_rating INT CHECK (resident_rating >= 1 AND resident_rating <= 5),
    resident_feedback TEXT,
    rated_at TIMESTAMP,
    parent_duplicate_of_id BIGINT REFERENCES complaints(id) ON DELETE SET NULL
);

CREATE TABLE complaint_activities (
    id BIGSERIAL PRIMARY KEY,
    complaint_id BIGINT NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    actor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    actor_name VARCHAR(100),
    actor_role VARCHAR(50),
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    activity_type VARCHAR(50) NOT NULL,
    public_note TEXT,
    internal_note TEXT,
    is_internal_only BOOLEAN NOT NULL DEFAULT FALSE,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Performance Indexes
CREATE INDEX idx_complaints_flat_id ON complaints(flat_id);
CREATE INDEX idx_complaints_resident_id ON complaints(resident_id);
CREATE INDEX idx_complaints_status ON complaints(status);
CREATE INDEX idx_complaints_category ON complaints(category);
CREATE INDEX idx_complaints_priority ON complaints(priority);
CREATE INDEX idx_complaints_created_at ON complaints(created_at DESC);
CREATE INDEX idx_complaints_critical ON complaints(is_critical_safety_escalation);
CREATE INDEX idx_complaints_parent_dup ON complaints(parent_duplicate_of_id);
CREATE INDEX idx_complaint_act_complaint ON complaint_activities(complaint_id, timestamp ASC);

