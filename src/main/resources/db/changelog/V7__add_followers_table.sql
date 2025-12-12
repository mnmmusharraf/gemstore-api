CREATE TABLE followers (
                           id SERIAL PRIMARY KEY,
                           follower_id INT NOT NULL REFERENCES users_table(id) ON DELETE CASCADE,
                           following_id INT NOT NULL REFERENCES users_table(id) ON DELETE CASCADE,
                           status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, PENDING (for private accounts)
                           created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
                           UNIQUE(follower_id, following_id)
);

CREATE INDEX idx_followers_follower_id ON followers(follower_id);
CREATE INDEX idx_followers_following_id ON followers(following_id);

COMMENT ON COLUMN followers.follower_id IS 'The user who is following';
COMMENT ON COLUMN followers. following_id IS 'The user being followed';