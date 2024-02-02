CREATE OR REPLACE FUNCTION notify_new_block_in_queue()
    RETURNS TRIGGER AS $$
DECLARE
    last_block_id INTEGER;
begin
    SELECT MAX(id) INTO last_block_id FROM block;
    IF TG_OP = 'INSERT' AND last_block_id = new.id THEN
        PERFORM pg_notify('new_block', NEW.id::text);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER notification_block_added_in_queue
    AFTER INSERT OR UPDATE ON block
    FOR EACH ROW
EXECUTE FUNCTION notify_new_block_in_queue()
