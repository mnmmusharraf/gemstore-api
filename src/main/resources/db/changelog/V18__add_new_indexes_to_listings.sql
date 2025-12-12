-- Search index
CREATE INDEX idx_listings_search ON listings USING GIN(search_vector);

-- ML queries
CREATE INDEX idx_listings_sold ON listings(is_sold) WHERE is_sold = TRUE;

-- Price history
CREATE INDEX idx_price_history_listing ON listing_price_history(listing_id);

-- Views
CREATE INDEX idx_views_listing ON listing_views(listing_id);