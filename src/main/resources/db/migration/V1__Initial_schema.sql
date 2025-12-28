-- Initial schema for Werewolf plugin
-- This migration creates the werewolf_players table

CREATE TABLE IF NOT EXISTS ${tablePrefix}werewolf_players (
    uuid VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    is_werewolf BOOLEAN NOT NULL DEFAULT FALSE,
    transformation_state VARCHAR(50) NOT NULL DEFAULT 'HUMAN',
    werewolf_type VARCHAR(50) NOT NULL DEFAULT 'ALPHA',
    original_skin_value VARCHAR(1000),
    original_skin_signature VARCHAR(1000),
    last_transformation_time BIGINT NOT NULL DEFAULT 0,
    last_cure_time BIGINT NOT NULL DEFAULT 0
);

