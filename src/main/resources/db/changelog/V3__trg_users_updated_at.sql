-- Create a trigger to execute the function before any update on the users_table
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users_table
    FOR EACH ROW
EXECUTE FUNCTION set_users_updated_at();