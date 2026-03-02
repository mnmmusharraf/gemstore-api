-- ============================================================================
--  Enhance Messages Table for Real-time Messaging with Kafka
-- ============================================================================

-- Step 1: Drop existing indexes that might conflict
DROP INDEX IF EXISTS idx_messages_sender_id;
DROP INDEX IF EXISTS idx_messages_receiver_id;
DROP INDEX IF EXISTS idx_messages_created_at;

-- Step 2: Add new columns first
ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS message_type VARCHAR(20) DEFAULT 'TEXT',
    ADD COLUMN IF NOT EXISTS attachment_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS listing_id BIGINT,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'SENT',
    ADD COLUMN IF NOT EXISTS read_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE;

-- Step 3: Migrate is_read data to status before dropping (only if is_read exists)
DO $$
    BEGIN
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'messages' AND column_name = 'is_read'
        ) THEN
            UPDATE messages SET status = 'READ', read_at = created_at WHERE is_read = TRUE;
            UPDATE messages SET status = 'SENT' WHERE is_read = FALSE;
        END IF;
    END $$;

-- Step 4: Drop old is_read column
ALTER TABLE messages DROP COLUMN IF EXISTS is_read;

-- Step 5: Change ID types to BIGINT (using PERFORM instead of SELECT)
DO $$
    DECLARE
        max_id BIGINT;
    BEGIN
        -- Alter id column
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'messages' AND column_name = 'id' AND data_type = 'integer'
        ) THEN
            -- Alter the column type
            ALTER TABLE messages ALTER COLUMN id TYPE BIGINT;

            -- Get max id and set sequence
            SELECT COALESCE(MAX(id), 1) INTO max_id FROM messages;

            -- Update the existing sequence
            PERFORM setval(pg_get_serial_sequence('messages', 'id'), max_id);
        END IF;

        -- Alter sender_id column
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'messages' AND column_name = 'sender_id' AND data_type = 'integer'
        ) THEN
            ALTER TABLE messages ALTER COLUMN sender_id TYPE BIGINT;
        END IF;

        -- Alter receiver_id column
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'messages' AND column_name = 'receiver_id' AND data_type = 'integer'
        ) THEN
            ALTER TABLE messages ALTER COLUMN receiver_id TYPE BIGINT;
        END IF;
    END $$;

-- Step 6: Set default values for any NULL status
UPDATE messages SET status = 'SENT' WHERE status IS NULL;

-- Step 7: Add NOT NULL constraints where needed
ALTER TABLE messages
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN message_type SET NOT NULL,
    ALTER COLUMN is_deleted SET NOT NULL;

-- Step 8: Create or replace trigger function for updated_at
CREATE OR REPLACE FUNCTION update_messages_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_messages_updated_at ON messages;
CREATE TRIGGER trigger_messages_updated_at
    BEFORE UPDATE ON messages
    FOR EACH ROW
EXECUTE FUNCTION update_messages_updated_at_column();

-- Step 9: Create optimized indexes
CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_receiver_id ON messages(receiver_id);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON messages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(sender_id, receiver_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_conversation_reverse ON messages(receiver_id, sender_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_unread ON messages(receiver_id, created_at DESC) WHERE is_deleted = FALSE AND status != 'READ';
CREATE INDEX IF NOT EXISTS idx_messages_listing ON messages(listing_id) WHERE listing_id IS NOT NULL;

-- Step 10: Add check constraints
ALTER TABLE messages DROP CONSTRAINT IF EXISTS chk_message_type;
ALTER TABLE messages ADD CONSTRAINT chk_message_type CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE', 'LISTING'));

ALTER TABLE messages DROP CONSTRAINT IF EXISTS chk_status;
ALTER TABLE messages ADD CONSTRAINT chk_status CHECK (status IN ('SENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED'));

-- Step 11: Add foreign key for listing_id
ALTER TABLE messages ADD CONSTRAINT fk_messages_listing FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE SET NULL;