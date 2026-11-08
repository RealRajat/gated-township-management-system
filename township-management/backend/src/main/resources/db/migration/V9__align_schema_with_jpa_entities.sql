-- =============================================================================
-- V9: Align the initial SQL schema with the JPA entity model.
--
-- These are plain PostgreSQL initialisation scripts (not Flyway migrations).
-- V9 is safe after V1-V8 on a fresh database and is idempotent.
-- =============================================================================

ALTER TABLE flat_members
    ADD COLUMN IF NOT EXISTS is_primary_contact BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE flats
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE towers
    ADD COLUMN IF NOT EXISTS floors INTEGER NOT NULL DEFAULT 15,
    ADD COLUMN IF NOT EXISTS total_units INTEGER NOT NULL DEFAULT 86,
    ADD COLUMN IF NOT EXISTS type VARCHAR(50) DEFAULT 'Residential',
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE towers
    ALTER COLUMN total_floors SET DEFAULT 15,
    ALTER COLUMN total_flats SET DEFAULT 86;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE
        DEFAULT CURRENT_TIMESTAMP;

-- JPA entities use LocalDateTime for these fields. Convert only columns that
-- are currently TIMESTAMPTZ, preserving the instant as UTC wall-clock time.
DO $$
DECLARE
    col RECORD;
BEGIN
    FOR col IN
        SELECT table_name, column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND data_type = 'timestamp with time zone'
          AND (table_name, column_name) IN (
              ('amenities', 'created_at'),
              ('amenities', 'updated_at'),
              ('amenity_bookings', 'cancelled_at'),
              ('amenity_bookings', 'created_at'),
              ('amenity_bookings', 'updated_at'),
              ('notice_reads', 'read_at'),
              ('notices', 'publish_date'),
              ('notices', 'expiry_date'),
              ('notices', 'created_at'),
              ('notices', 'updated_at'),
              ('visitor_activities', 'timestamp'),
              ('visitors', 'valid_from'),
              ('visitors', 'valid_until'),
              ('visitors', 'check_in_time'),
              ('visitors', 'check_out_time'),
              ('visitors', 'photo_captured_at'),
              ('visitors', 'photo_deleted_at'),
              ('visitors', 'approved_at'),
              ('visitors', 'created_at'),
              ('visitors', 'updated_at')
          )
    LOOP
        EXECUTE format(
            'ALTER TABLE %I ALTER COLUMN %I TYPE TIMESTAMP WITHOUT TIME ZONE USING %I AT TIME ZONE ''UTC''',
            col.table_name,
            col.column_name,
            col.column_name
        );
    END LOOP;
END $$;
