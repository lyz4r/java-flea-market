ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(72) NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

-- Pre-created administrator (cannot be registered via the API).
-- Test credentials: admin / admin
INSERT INTO users (login, name, email, password_hash, role, blocked, created_at, updated_at)
VALUES ('admin', 'Administrator', 'admin@flea.market',
        '$2a$10$Q.5GWyKo2EuaX5tdnlBZgOs9cL/v.awhyU7agcJOu39/jMdEJSale',
        'ADMIN', FALSE, NOW(), NOW());
