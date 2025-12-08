CREATE TABLE reports (
                         id SERIAL PRIMARY KEY,
                         reporter_id INT NOT NULL REFERENCES users_table(id) ON DELETE CASCADE,
                         reported_user_id INT REFERENCES users_table(id) ON DELETE SET NULL,
                         reported_post_id INT REFERENCES posts(id) ON DELETE SET NULL,
                         reason VARCHAR(100) NOT NULL, -- 'fraud', 'fake', 'abuse', 'other'
                         details TEXT,
                         status VARCHAR(30) DEFAULT 'PENDING', -- PENDING, REVIEWED, RESOLVED, DISMISSED
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
                         reviewed_at TIMESTAMP WITH TIME ZONE,
                         reviewed_by INT REFERENCES users_table(id)
);

CREATE INDEX idx_reports_reporter_id ON reports(reporter_id);
CREATE INDEX idx_reports_status ON reports(status);