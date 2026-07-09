CREATE TABLE IF NOT EXISTS ads (
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(19, 2),
    status VARCHAR(32) NOT NULL,
    admin_deactivated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ads_status ON ads(status);
CREATE INDEX idx_ads_author_id ON ads(author_id);
CREATE INDEX idx_ads_created_at ON ads(created_at DESC);
