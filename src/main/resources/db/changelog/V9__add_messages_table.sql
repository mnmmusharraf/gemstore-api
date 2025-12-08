CREATE TABLE messages (
                          id SERIAL PRIMARY KEY,
                          sender_id INT NOT NULL REFERENCES users_table(id) ON DELETE CASCADE,
                          receiver_id INT NOT NULL REFERENCES users_table(id) ON DELETE CASCADE,
                          content TEXT NOT NULL,
                          is_read BOOLEAN DEFAULT FALSE NOT NULL,
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
                          deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_messages_sender_id ON messages(sender_id);
CREATE INDEX idx_messages_receiver_id ON messages(receiver_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);