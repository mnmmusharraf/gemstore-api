-- Add profile-related columns
ALTER TABLE users_table
    ADD COLUMN IF NOT EXISTS website VARCHAR(500),
    ADD COLUMN IF NOT EXISTS bio TEXT,
    ADD COLUMN IF NOT EXISTS is_private BOOLEAN DEFAULT FALSE NOT NULL;

-- Add social stats (denormalized for performance; update via triggers or app logic)
ALTER TABLE users_table
    ADD COLUMN IF NOT EXISTS posts_count INT DEFAULT 0 NOT NULL,
    ADD COLUMN IF NOT EXISTS followers_count INT DEFAULT 0 NOT NULL,
    ADD COLUMN IF NOT EXISTS following_count INT DEFAULT 0 NOT NULL;

-- Add comments for clarity
COMMENT ON COLUMN users_table.website IS 'User personal or business website URL';
COMMENT ON COLUMN users_table.bio IS 'Short user biography or description';
COMMENT ON COLUMN users_table.is_private IS 'If true, only approved followers can see content';
COMMENT ON COLUMN users_table.posts_count IS 'Cached count of user posts';
COMMENT ON COLUMN users_table.followers_count IS 'Cached count of followers';
COMMENT ON COLUMN users_table.following_count IS 'Cached count of users this user follows';