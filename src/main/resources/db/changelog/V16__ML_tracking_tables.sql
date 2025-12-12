-- Price History (Auto-tracked when price changes)
CREATE TABLE listing_price_history (
                                       id BIGSERIAL PRIMARY KEY,
                                       listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
                                       old_price DECIMAL(14,2),
                                       new_price DECIMAL(14,2) NOT NULL,
                                       change_reason VARCHAR(20),
                                       created_at TIMESTAMP DEFAULT NOW()
);

-- View Tracking
CREATE TABLE listing_views (
                               id BIGSERIAL PRIMARY KEY,
                               listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
                               user_id BIGINT REFERENCES users_table(id),
                               viewed_at TIMESTAMP DEFAULT NOW()
);

-- Sale Records (For ML training)
CREATE TABLE listing_sales (
                               id BIGSERIAL PRIMARY KEY,
                               listing_id BIGINT NOT NULL REFERENCES listings(id),
                               seller_id BIGINT NOT NULL REFERENCES users_table(id),
                               buyer_id BIGINT REFERENCES users_table(id),
                               listed_price DECIMAL(14,2) NOT NULL,
                               sold_price DECIMAL(14,2) NOT NULL,
                               days_on_market INT,
                               listing_snapshot JSONB,
                               created_at TIMESTAMP DEFAULT NOW()
);