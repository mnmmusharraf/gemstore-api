CREATE OR REPLACE FUNCTION update_reports_updated_at()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_reports
    BEFORE UPDATE ON reports
    FOR EACH ROW
EXECUTE FUNCTION update_reports_updated_at();