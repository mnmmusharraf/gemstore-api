-- Create a function to automatically update the updated_at timestamp
CREATE OR REPLACE FUNCTION set_users_updated_at()
    RETURNS TRIGGER AS $func$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$func$ LANGUAGE plpgsql;
$func$