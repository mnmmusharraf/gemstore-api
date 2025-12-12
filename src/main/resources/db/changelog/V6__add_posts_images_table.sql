CREATE TABLE post_images (
                             id SERIAL PRIMARY KEY,
                             post_id INT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                             image_url VARCHAR(500) NOT NULL,
                             display_order INT DEFAULT 0,
                             created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_post_images_post_id ON post_images(post_id);