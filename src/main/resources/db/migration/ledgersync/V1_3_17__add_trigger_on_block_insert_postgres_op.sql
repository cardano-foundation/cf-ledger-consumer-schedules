CREATE OR REPLACE FUNCTION notify_new_block_in_queue()
    RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        PERFORM pg_notify('new_block', NEW.hash::text);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER notification_block_added_in_queue
    AFTER INSERT OR UPDATE ON block
    FOR EACH ROW
EXECUTE FUNCTION notify_new_block_in_queue()
