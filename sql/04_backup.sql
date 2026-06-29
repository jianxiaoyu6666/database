-- 备份数据用:
-- select是读表数据的基本权限
-- show view用于导出视图的“创建语句”：create view
-- trigger用于导出触发器的定义
-- lock tables用于备份的时候进行锁表，不加的话备份过程中如果有写入，导出的数据可能不一致
create user 'backup_operator'@'localhost' identified by '123';
-- 授权语句格式为grant 权限列表 on 数据库名.表名 to
grant select,show view,trigger,lock tables on train_ticket_db.* to 'backup_operator'@'localhost';


-- 恢复数据用，此用户一般情况下不启用,它具有管理数据库的最高权限，除非数据库遭到破坏时才启用并恢复备份 --
-- 删掉旧的
DROP USER IF EXISTS 'restore_operator'@'localhost';

CREATE USER 'restore_operator'@'localhost' IDENTIFIED BY '456';
GRANT ALL PRIVILEGES ON train_ticket_db.* TO 'restore_operator'@'localhost';
GRANT SUPER ON *.* TO 'restore_operator'@'localhost';    -- ← 加这行
ALTER USER 'restore_operator'@'localhost' ACCOUNT LOCK;




create procedure sp_backup_database(
    IN p_file_name VARCHAR(255),
    IN p_file_size BIGINT
)
BEGIN
    INSERT INTO audit_log (table_name, operation_type, record_id,
                           new_value, operated_at, log_date)
    VALUES ('SYSTEM', 'INSERT', 0,
            JSON_OBJECT('action', 'backup',
                        'file', p_file_name,
                        'size', p_file_size,
                        'timestamp', NOW()),
            NOW(), CURDATE());
END
