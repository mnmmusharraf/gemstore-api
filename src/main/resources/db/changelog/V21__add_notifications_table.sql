-- ===== NOTIFICATIONS TABLE =====
CREATE TABLE notifications (
                               id SERIAL PRIMARY KEY,
                               user_id INT NOT NULL REFERENCES users_table(id) ON DELETE CASCADE,
                               actor_id INT REFERENCES users_table(id) ON DELETE SET NULL,
                               listing_id INT REFERENCES listings(id) ON DELETE SET NULL,
                               type VARCHAR(50) NOT NULL,
                               message TEXT,
                               is_read BOOLEAN DEFAULT FALSE NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- Indexes for performance
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_type ON notifications(type);

-- Comments
COMMENT ON TABLE notifications IS 'User notifications for likes, follows, comments, etc.';
COMMENT ON COLUMN notifications.user_id IS 'The user who receives the notification';
COMMENT ON COLUMN notifications.actor_id IS 'The user who triggered the notification (who liked, followed, etc.)';
COMMENT ON COLUMN notifications.listing_id IS 'Related listing (for likes, comments on listings)';
COMMENT ON COLUMN notifications.type IS 'LIKE, FOLLOW, FOLLOW_REQUEST, FOLLOW_ACCEPTED, COMMENT, MENTION, etc.';
COMMENT ON COLUMN notifications.is_read IS 'Whether the user has seen/read this notification';