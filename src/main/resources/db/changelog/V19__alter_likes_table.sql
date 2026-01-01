CREATE TABLE likes (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users_table(id) ON DELETE CASCADE,
    listing_id INT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    UNIQUE(user_id, listing_id)
);
CREATE INDEX idx_likes_listing_id ON likes(listing_id);
CREATE INDEX idx_likes_user_id ON likes(user_id);