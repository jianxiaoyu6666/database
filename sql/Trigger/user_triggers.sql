DROP TRIGGER IF EXISTS trg_user_audit_insert;

DELIMITER $$

CREATE TRIGGER trg_user_audit_insert
    AFTER INSERT
    ON user
    FOR EACH ROW
BEGIN
    INSERT INTO audit_log(table_name, operation_type, record_id, old_value, new_value, operated_by, operated_at, log_date)
    VALUES (
               'user',
               'INSERT',
               NEW.id,
               NULL,
               JSON_OBJECT(
                       'id', NEW.id,
                       'username', NEW.username,
                       'password', NEW.password,
                       'real_name', NEW.real_name,
                       'id_number', NEW.id_number,
                       'phone', NEW.phone,
                       'email', NEW.email,
                       'user_type', NEW.user_type,
                       'status', NEW.status
               ),
               NEW.id,
               NOW(),
               CURDATE()
           );
    END$$

    DELIMITER ;