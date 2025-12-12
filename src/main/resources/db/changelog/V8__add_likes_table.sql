CREATE TABLE likes (
                       id SERIAL PRIMARY KEY,
                       user_id INT NOT NULL REFERENCES users_table(id) ON DELETE CASCADE,
                       post_id INT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
                       UNIQUE(user_id, post_id)
);

CREATE INDEX idx_likes_post_id ON likes(post_id);
CREATE INDEX idx_likes_user_id ON likes(user_id);