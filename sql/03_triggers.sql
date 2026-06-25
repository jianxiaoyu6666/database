-- ============================================
-- 火车售票管理系统 - 触发器
-- 实现数据自动维护、审计日志、业务规则检查
--
-- 重要说明: MySQL分区表不支持FOREIGN KEY约束
-- 因此通过触发器来补偿引用完整性和级联操作:
--   - trg_schedule_cancel_cleanup: 排班取消时级联取消订单 (替代ON DELETE CASCADE)
--   - trg_order_release_seat: 退票时自动释放库存 (替代ON DELETE CASCADE)
--   - trg_schedule_init_inventory: 排班创建时初始化库存 (保证数据一致性)
-- ============================================

USE train_ticket_db;

-- ============================================
-- 1. 用户表增删改审计触发器
-- ============================================
DELIMITER //

CREATE TRIGGER trg_user_audit_insert
AFTER INSERT ON `user` FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, operation_type, record_id, new_value, operated_at, log_date)
    VALUES ('user', 'INSERT', NEW.id,
            JSON_OBJECT('username', NEW.username, 'real_name', NEW.real_name, 'user_type', NEW.user_type),
            NOW(), CURDATE());
END //

CREATE TRIGGER trg_user_audit_update
AFTER UPDATE ON `user` FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, operation_type, record_id, old_value, new_value, operated_at, log_date)
    VALUES ('user', 'UPDATE', NEW.id,
            JSON_OBJECT('username', OLD.username, 'real_name', OLD.real_name, 'user_type', OLD.user_type, 'status', OLD.status),
            JSON_OBJECT('username', NEW.username, 'real_name', NEW.real_name, 'user_type', NEW.user_type, 'status', NEW.status),
            NOW(), CURDATE());
END //

CREATE TRIGGER trg_user_audit_delete
AFTER DELETE ON `user` FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, operation_type, record_id, old_value, operated_at, log_date)
    VALUES ('user', 'DELETE', OLD.id,
            JSON_OBJECT('username', OLD.username, 'real_name', OLD.real_name),
            NOW(), CURDATE());
END //

-- ============================================
-- 2. 车票状态变更审计触发器 (合并原 order_item 审计)
-- ============================================
CREATE TRIGGER trg_order_audit_update
AFTER UPDATE ON `ticket_order` FOR EACH ROW
BEGIN
    IF OLD.ticket_status != NEW.ticket_status THEN
        INSERT INTO audit_log (table_name, operation_type, record_id, old_value, new_value, operated_at, log_date)
        VALUES ('ticket_order', 'UPDATE', NEW.id,
                JSON_OBJECT('order_number', OLD.order_number, 'ticket_status', OLD.ticket_status, 'passenger_name', OLD.passenger_name),
                JSON_OBJECT('order_number', NEW.order_number, 'ticket_status', NEW.ticket_status, 'passenger_name', NEW.passenger_name),
                NOW(), CURDATE());
    END IF;
END //

-- ============================================
-- 3. 列车排班取消时清理相关车票触发器
-- ============================================
CREATE TRIGGER trg_schedule_cancel_cleanup
AFTER UPDATE ON `train_schedule` FOR EACH ROW
BEGIN
    IF OLD.status != 'CANCELLED' AND NEW.status = 'CANCELLED' THEN
        -- 将相关车票标记为已取消
        UPDATE ticket_order SET ticket_status = 'CANCELLED'
        WHERE train_schedule_id = NEW.id AND ticket_status = 'VALID';

        -- 记录审计日志
        INSERT INTO audit_log (table_name, operation_type, record_id,
            new_value, operated_at, log_date)
        VALUES ('train_schedule', 'CANCEL', NEW.id,
                JSON_OBJECT('train_id', NEW.train_id, 'schedule_date', NEW.schedule_date, 'action', '班次取消，相关车票已取消'),
                NOW(), CURDATE());
    END IF;
END //

-- ============================================
-- 4. 退票/取消时自动释放座位库存触发器
-- ============================================
CREATE TRIGGER trg_order_release_seat
AFTER UPDATE ON `ticket_order` FOR EACH ROW
BEGIN
    IF NEW.ticket_status IN ('CANCELLED', 'REFUNDED') AND OLD.ticket_status = 'VALID' THEN
        UPDATE seat_inventory SET is_occupied = 0, order_id = NULL
        WHERE order_id = NEW.id;
    END IF;
END //

-- ============================================
-- 6. 列车排班创建时自动初始化座位库存触发器 (AFTER INSERT)
-- 注意: MySQL不允许在触发器中修改触发它的同一张表,
--       所以 total_seats/available_seats 由 BEFORE INSERT 触发器 trg_schedule_before_insert 计算
-- ============================================
CREATE TRIGGER trg_schedule_init_inventory
AFTER INSERT ON `train_schedule` FOR EACH ROW
BEGIN
    DECLARE v_max_station_order INT;

    -- 获取该车次的最大站点序号
    SELECT MAX(station_order) INTO v_max_station_order
    FROM train_route WHERE train_id = NEW.train_id;

    -- 为所有座位初始化全部区间库存 (批量插入，性能优化)
    INSERT INTO seat_inventory (train_schedule_id, carriage_id, seat_id, from_station_order, to_station_order, is_occupied, schedule_date)
    SELECT
        NEW.id,
        c.id,
        s.id,
        1,
        v_max_station_order,
        0,
        NEW.schedule_date
    FROM carriage c
    JOIN seat s ON s.carriage_id = c.id
    WHERE c.train_id = NEW.train_id;
END //

-- ============================================
-- 6.1 排班创建前自动计算 total_seats 和 available_seats (BEFORE INSERT)
-- ============================================
CREATE TRIGGER trg_schedule_before_insert
BEFORE INSERT ON `train_schedule` FOR EACH ROW
BEGIN
    DECLARE v_seat_count INT DEFAULT 0;

    SELECT COUNT(*) INTO v_seat_count
    FROM seat s
    JOIN carriage c ON s.carriage_id = c.id
    WHERE c.train_id = NEW.train_id;

    -- 如果 INSERT 未显式提供，则自动计算
    IF NEW.total_seats IS NULL OR NEW.total_seats = 0 THEN
        SET NEW.total_seats = v_seat_count;
    END IF;
    IF NEW.available_seats IS NULL OR NEW.available_seats = 0 THEN
        SET NEW.available_seats = v_seat_count;
    END IF;
END //

-- ============================================
-- 5. 购票后自动更新剩余座位数触发器 (seat_inventory 变更时)
-- ============================================
CREATE TRIGGER trg_seat_occupied_update
AFTER UPDATE ON `seat_inventory` FOR EACH ROW
BEGIN
    IF OLD.is_occupied = 0 AND NEW.is_occupied = 1 THEN
        UPDATE train_schedule SET available_seats = available_seats - 1
        WHERE id = NEW.train_schedule_id;
    ELSEIF OLD.is_occupied = 1 AND NEW.is_occupied = 0 THEN
        UPDATE train_schedule SET available_seats = available_seats + 1
        WHERE id = NEW.train_schedule_id;
    END IF;
END //

DELIMITER ;
