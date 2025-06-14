-- Create services table
CREATE TABLE IF NOT EXISTS services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create teams table
CREATE TABLE IF NOT EXISTS teams (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    service_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_service
        FOREIGN KEY(service_id)
        REFERENCES services(id)
        ON DELETE RESTRICT, -- Or ON DELETE CASCADE depending on desired behavior
    CONSTRAINT uq_team_name_service_id UNIQUE (name, service_id)
);

-- Create user_teams join table
-- Assuming 'users' table already exists from a previous migration or setup
CREATE TABLE IF NOT EXISTS user_teams (
    user_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, team_id),
    CONSTRAINT fk_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_team
        FOREIGN KEY(team_id)
        REFERENCES teams(id)
        ON DELETE CASCADE
);

-- Optional: Add triggers to automatically update `updated_at` timestamps
-- This syntax is for PostgreSQL. Adjust for other databases if necessary.

-- For services table
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_services_updated_at
BEFORE UPDATE ON services
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- For teams table
CREATE TRIGGER update_teams_updated_at
BEFORE UPDATE ON teams
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Note: `user_teams` table typically doesn't need `updated_at` unless tracking membership changes.
-- If `created_at` on `user_teams` should reflect the time of addition, DEFAULT CURRENT_TIMESTAMP is sufficient.
-- No `updated_at` trigger is added for `user_teams` by default.

-- Indexes for foreign keys can be beneficial for performance, though some DBs create them automatically.
CREATE INDEX IF NOT EXISTS idx_teams_service_id ON teams(service_id);
CREATE INDEX IF NOT EXISTS idx_user_teams_user_id ON user_teams(user_id);
CREATE INDEX IF NOT EXISTS idx_user_teams_team_id ON user_teams(team_id);
