CREATE TABLE posts (
                       id SERIAL PRIMARY KEY,
                       user_id INT NOT NULL REFERENCES users_table(id) ON DELETE CASCADE,
                       title VARCHAR(255) NOT NULL,
                       description TEXT,
                       gemstone_type VARCHAR(100),
                       carat_weight DECIMAL(10, 2),
                       color VARCHAR(100),
                       clarity VARCHAR(100),
                       origin VARCHAR(100),
                       price DECIMAL(12, 2),
                       currency VARCHAR(10) DEFAULT 'USD',
                       status VARCHAR(30) DEFAULT 'ACTIVE', -- ACTIVE, SOLD, DRAFT, DELETED
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
                       deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_status ON posts(status);