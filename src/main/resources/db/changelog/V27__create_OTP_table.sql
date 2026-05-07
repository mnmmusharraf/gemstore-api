CREATE TABLE email_verification_otp (
                                        id SERIAL PRIMARY KEY,
                                        user_id INT REFERENCES users_table(id) ON DELETE CASCADE,
                                        otp_code VARCHAR(10) NOT NULL,
                                        expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                        attempts INT DEFAULT 0,
                                        created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);