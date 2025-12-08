CREATE OR REPLACE FUNCTION update_follower_counts()
    RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE users_table SET following_count = following_count + 1 WHERE id = NEW.follower_id;
        UPDATE users_table SET followers_count = followers_count + 1 WHERE id = NEW.following_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE users_table SET following_count = following_count - 1 WHERE id = OLD.follower_id;
        UPDATE users_table SET followers_count = followers_count - 1 WHERE id = OLD.following_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_follower_counts
    AFTER INSERT OR DELETE ON followers
    FOR EACH ROW
EXECUTE FUNCTION update_follower_counts();