CREATE TABLE IF NOT EXISTS tb_blacklisted_token (
    token_hash VARCHAR(64) PRIMARY KEY,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tb_blacklisted_token_expires_at
ON tb_blacklisted_token (expires_at);
