
-- 1. 用户表增删改审计触发器  这里是按照提供sql代码的同学的代码改的（因为有一些地方不太合适）
DROP TRIGGER IF EXISTS trg_user_audit_insert;
DELIMITER $$
-- 用户注册时增加日志 --
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

-- 用户更新信息时增加日志 --
DROP TRIGGER IF EXISTS trg_user_audit_update;
DELIMITER $$

CREATE TRIGGER trg_user_audit_update
    AFTER UPDATE ON `user` FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, operation_type, record_id, old_value, new_value, operated_by,operated_at, log_date)
    VALUES ('user', 'UPDATE', NEW.id,
            JSON_OBJECT('username', OLD.username, 'real_name', OLD.real_name, 'user_type', OLD.user_type, 'status', OLD.status),
            JSON_OBJECT('username', NEW.username, 'real_name', NEW.real_name, 'user_type', NEW.user_type, 'status', NEW.status),
            COALESCE(@audit_operator, NEW.id),
            NOW(), CURDATE());
END $$


DELIMITER //
DROP TRIGGER IF EXISTS trg_user_audit_delete;
CREATE TRIGGER trg_user_audit_delete
    AFTER DELETE ON `user` FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, operation_type, record_id, old_value,operated_by ,operated_at, log_date)
    VALUES ('user', 'DELETE', OLD.id,
            JSON_OBJECT('username', OLD.username, 'real_name', OLD.real_name),
            COALESCE(@audit_operator, OLD.id),
            NOW(), CURDATE());
END //
