CREATE TABLE reports (
                         id BIGSERIAL PRIMARY KEY,

    -- Reporter info
                         reporter_id BIGINT NOT NULL REFERENCES users_table(id) ON DELETE CASCADE,

    -- What is being reported (only ONE must be set)
                         reported_listing_id BIGINT REFERENCES listings(id) ON DELETE CASCADE,
                         reported_user_id BIGINT REFERENCES users_table(id) ON DELETE CASCADE,
                         reported_message_id BIGINT REFERENCES messages(id) ON DELETE CASCADE,

    -- Report details
                         report_type VARCHAR(50) NOT NULL,  -- LISTING, USER, MESSAGE
                         reason VARCHAR(100) NOT NULL,      -- SPAM, FRAUD, INAPPROPRIATE, etc.
                         description TEXT,

    -- Status tracking
                         status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, REVIEWING, RESOLVED, DISMISSED

    -- Admin handling
                         reviewed_by BIGINT REFERENCES users_table(id),
                         reviewed_at TIMESTAMP,
                         admin_notes TEXT,
                         action_taken VARCHAR(100),  -- WARNING_ISSUED, LISTING_REMOVED, USER_BANNED, NO_ACTION

    -- Timestamps
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Ensure only ONE target is reported
                         CONSTRAINT chk_report_target CHECK (
                             (reported_listing_id IS NOT NULL AND reported_user_id IS NULL AND reported_message_id IS NULL) OR
                             (reported_listing_id IS NULL AND reported_user_id IS NOT NULL AND reported_message_id IS NULL) OR
                             (reported_listing_id IS NULL AND reported_user_id IS NULL AND reported_message_id IS NOT NULL)
                             ),

    -- Prevent duplicate reports
                         CONSTRAINT uq_report_unique UNIQUE (
                                                             reporter_id,
                                                             reported_listing_id,
                                                             reported_user_id,
                                                             reported_message_id
                             )
);

CREATE INDEX idx_reports_reporter ON reports(reporter_id);
CREATE INDEX idx_reports_listing ON reports(reported_listing_id);
CREATE INDEX idx_reports_user ON reports(reported_user_id);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_created ON reports(created_at DESC);