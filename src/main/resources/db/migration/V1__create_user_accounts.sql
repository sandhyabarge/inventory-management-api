CREATE TABLE user_accounts (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    role VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_role
        CHECK (role IN ('ADMIN', 'INVENTORY_MANAGER', 'PURCHASING_AGENT', 'VIEWER'))
);

CREATE INDEX idx_user_accounts_role ON user_accounts(role);
CREATE INDEX idx_user_accounts_active ON user_accounts(active);
