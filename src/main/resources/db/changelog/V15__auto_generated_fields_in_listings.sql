-- Columns to listings table
ALTER TABLE listings ADD COLUMN listing_number VARCHAR(20) UNIQUE;
ALTER TABLE listings ADD COLUMN price_per_carat DECIMAL(14,2)
    GENERATED ALWAYS AS (
        CASE WHEN carat_weight > 0 THEN price / carat_weight ELSE NULL END
        ) STORED;
ALTER TABLE listings ADD COLUMN completeness_score INT DEFAULT 0;
ALTER TABLE listings ADD COLUMN days_to_sell INT;
ALTER TABLE listings ADD COLUMN search_vector TSVECTOR;