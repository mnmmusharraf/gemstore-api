-- Trigger on posts table
CREATE TRIGGER trg_update_posts_count
    AFTER INSERT OR DELETE ON posts
    FOR EACH ROW
EXECUTE FUNCTION update_user_posts_count();