-- Create the users_table with all columns defined at once
CREATE TABLE users_table (
                             id SERIAL PRIMARY KEY,
                             display_name VARCHAR(150) NOT NULL,
                             email VARCHAR(200),
                             username VARCHAR(200),
                             password_hash VARCHAR(255),
                             first_name VARCHAR(100),
                             last_name VARCHAR(100),
                             provider VARCHAR(30) DEFAULT 'LOCAL' NOT NULL,
                             provider_id VARCHAR(255),
                             email_verified BOOLEAN DEFAULT FALSE NOT NULL,
                             role VARCHAR(50) DEFAULT 'USER' NOT NULL,
                             avatar_url VARCHAR(500),
                             status VARCHAR(30) DEFAULT 'ACTIVE' NOT NULL,
                             failed_login_attempts INT DEFAULT 0 NOT NULL,
                             locked_until TIMESTAMP WITH TIME ZONE,
                             last_login_at TIMESTAMP WITH TIME ZONE,
                             password_changed_at TIMESTAMP WITH TIME ZONE,
                             mfa_enabled BOOLEAN DEFAULT FALSE NOT NULL,
                             timezone VARCHAR(64),
                             locale VARCHAR(16),
                             created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
                             updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
                             deleted_at TIMESTAMP WITH TIME ZONE
);

-- Add comments to the columns for better understanding
COMMENT ON COLUMN users_table.provider IS 'Authentication provider (e.g., LOCAL, GOOGLE, FACEBOOK)';
COMMENT ON COLUMN users_table.status IS 'User account status (e.g., ACTIVE, INACTIVE, SUSPENDED)';

-- Create unique indexes for email, username, and provider IDs
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_ci ON users_table (lower(email)) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username_ci ON users_table (lower(username)) WHERE username IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_provider_provider_id ON users_table (provider, provider_id) WHERE provider_id IS NOT NULL;