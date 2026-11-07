-- ==============================================================================
-- V1__init_core_entities.sql
-- Baseline schema for core township entities: towers, flats, users, flat_members
-- ==============================================================================

CREATE TABLE IF NOT EXISTS towers (
    id VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    total_floors INTEGER NOT NULL,
    total_flats INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS flats (
    id BIGSERIAL PRIMARY KEY,
    tower_id VARCHAR(10) NOT NULL REFERENCES towers(id) ON DELETE RESTRICT,
    flat_number VARCHAR(20) NOT NULL,
    floor_number INTEGER NOT NULL,
    area_sq_ft DOUBLE PRECISION NOT NULL,
    occupancy_status VARCHAR(30) NOT NULL DEFAULT 'OWNER_OCCUPIED',
    CONSTRAINT unique_tower_flat UNIQUE (tower_id, flat_number)
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(30) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS flat_members (
    id BIGSERIAL PRIMARY KEY,
    flat_id BIGINT NOT NULL REFERENCES flats(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    member_type VARCHAR(30) NOT NULL DEFAULT 'PRIMARY_OWNER',
    is_primary BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_flat_user UNIQUE (flat_id, user_id)
);

