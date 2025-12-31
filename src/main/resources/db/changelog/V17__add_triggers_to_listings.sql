-- Auto-generate listing number
CREATE OR REPLACE FUNCTION generate_listing_number()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.listing_number := 'GEM-' || TO_CHAR(NOW(), 'YYYY') || '-' ||
                          LPAD(NEW.id::TEXT, 6, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER trg_listing_number
    AFTER INSERT ON listings
    FOR EACH ROW EXECUTE FUNCTION generate_listing_number();

-- Auto-update timestamp
CREATE OR REPLACE FUNCTION update_timestamp()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_timestamp
    BEFORE UPDATE ON listings
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- Auto-calculate completeness
CREATE OR REPLACE FUNCTION calculate_completeness()
    RETURNS TRIGGER AS $$
DECLARE
    score INT := 0;
BEGIN
    IF NEW.gemstone_type_id IS NOT NULL THEN score := score + 15; END IF;
    IF NEW.carat_weight IS NOT NULL THEN score := score + 15; END IF;
    IF NEW.price IS NOT NULL THEN score := score + 15; END IF;
    IF NEW.title IS NOT NULL THEN score := score + 15; END IF;
    IF NEW.color_id IS NOT NULL THEN score := score + 10; END IF;
    IF NEW.color_quality_id IS NOT NULL THEN score := score + 8; END IF;
    IF NEW.clarity_id IS NOT NULL THEN score := score + 8; END IF;
    IF NEW.cut_id IS NOT NULL THEN score := score + 6; END IF;
    IF NEW.origin_id IS NOT NULL THEN score := score + 8; END IF;

    NEW.completeness_score := score;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_completeness
    BEFORE INSERT OR UPDATE ON listings
    FOR EACH ROW EXECUTE FUNCTION calculate_completeness();

-- Auto-track price changes
CREATE OR REPLACE FUNCTION track_price_change()
    RETURNS TRIGGER AS $$
BEGIN
    IF OLD.price IS DISTINCT FROM NEW.price THEN
        INSERT INTO listing_price_history (listing_id, old_price, new_price, change_reason)
        VALUES (
                   NEW.id,
                   OLD.price,
                   NEW.price,
                   CASE WHEN NEW.price < OLD.price THEN 'REDUCTION' ELSE 'INCREASE' END
               );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_price_change
    AFTER UPDATE ON listings
    FOR EACH ROW EXECUTE FUNCTION track_price_change();

-- Auto-calculate days to sell
CREATE OR REPLACE FUNCTION calculate_days_to_sell()
    RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_sold = TRUE AND OLD.is_sold = FALSE THEN
        NEW.sold_at := NOW();
        NEW.days_to_sell := EXTRACT(DAY FROM (NOW() - NEW.created_at));
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_days_to_sell
    BEFORE UPDATE ON listings
    FOR EACH ROW EXECUTE FUNCTION calculate_days_to_sell();

-- Auto-update favorites count
CREATE OR REPLACE FUNCTION update_favorites_count()
    RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE listings SET favorites_count = favorites_count + 1
        WHERE id = NEW.listing_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE listings SET favorites_count = favorites_count - 1
        WHERE id = OLD.listing_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_favorites_count
    AFTER INSERT OR DELETE ON favorites
    FOR EACH ROW EXECUTE FUNCTION update_favorites_count();

-- Auto-update search vector
CREATE OR REPLACE FUNCTION update_search_vector()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
            setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
            setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_search_vector
    BEFORE INSERT OR UPDATE ON listings
    FOR EACH ROW EXECUTE FUNCTION update_search_vector();

