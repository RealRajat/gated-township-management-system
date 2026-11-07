-- ==============================================================================
-- V6__amenity_and_clubhouse_booking.sql
-- Schema for Phase 9: Amenity and Clubhouse Booking System
-- Pune Grand Vista Gated Community (28 Towers, 2,400 Units)
-- ==============================================================================

CREATE TABLE IF NOT EXISTS amenities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL CHECK (category IN ('FITNESS', 'SPORTS', 'LEISURE', 'EVENTS')),
    description TEXT,
    location VARCHAR(100),
    open_time TIME WITHOUT TIME ZONE NOT NULL DEFAULT '06:00:00',
    close_time TIME WITHOUT TIME ZONE NOT NULL DEFAULT '22:00:00',
    slot_duration_minutes INTEGER NOT NULL DEFAULT 60 CHECK (slot_duration_minutes > 0),
    slot_capacity INTEGER NOT NULL DEFAULT 10 CHECK (slot_capacity > 0),
    max_active_bookings_per_flat INTEGER NOT NULL DEFAULT 2 CHECK (max_active_bookings_per_flat > 0),
    cancel_deadline_hours INTEGER NOT NULL DEFAULT 2 CHECK (cancel_deadline_hours >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    maintenance_reason VARCHAR(255),
    rules_and_guidelines TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS amenity_bookings (
    id BIGSERIAL PRIMARY KEY,
    booking_reference VARCHAR(30) NOT NULL UNIQUE,
    amenity_id BIGINT NOT NULL REFERENCES amenities(id) ON DELETE RESTRICT,
    flat_id BIGINT NOT NULL REFERENCES flats(id) ON DELETE RESTRICT,
    resident_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    booking_date DATE NOT NULL,
    start_time TIME WITHOUT TIME ZONE NOT NULL,
    end_time TIME WITHOUT TIME ZONE NOT NULL,
    attendee_count INTEGER NOT NULL DEFAULT 1 CHECK (attendee_count > 0),
    purpose VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('CONFIRMED', 'CANCELLED', 'COMPLETED')),
    cancellation_reason VARCHAR(255),
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancelled_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_booking_time_order CHECK (end_time > start_time)
);

-- Performance indices
CREATE INDEX IF NOT EXISTS idx_amenities_code ON amenities(code);
CREATE INDEX IF NOT EXISTS idx_amenities_category ON amenities(category);
CREATE INDEX IF NOT EXISTS idx_amenities_is_active ON amenities(is_active);

CREATE INDEX IF NOT EXISTS idx_amenity_bookings_slot ON amenity_bookings(amenity_id, booking_date, start_time);
CREATE INDEX IF NOT EXISTS idx_amenity_bookings_flat ON amenity_bookings(flat_id, status);
CREATE INDEX IF NOT EXISTS idx_amenity_bookings_resident ON amenity_bookings(resident_id);
CREATE INDEX IF NOT EXISTS idx_amenity_bookings_date ON amenity_bookings(booking_date, status);
CREATE INDEX IF NOT EXISTS idx_amenity_bookings_ref ON amenity_bookings(booking_reference);

