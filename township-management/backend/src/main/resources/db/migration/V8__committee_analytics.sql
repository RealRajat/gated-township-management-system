-- ==============================================================================
-- V8__committee_analytics.sql
-- Update users role check constraint to include COMMITTEE
-- ==============================================================================

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('RESIDENT', 'HELP_DESK', 'GUARD', 'COMMITTEE'));

