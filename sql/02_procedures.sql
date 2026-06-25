-- ============================================
-- 火车售票管理系统 - 存储过程
-- 实现核心业务逻辑的封装
-- ============================================

USE train_ticket_db;

-- ============================================
-- 1. 购票存储过程 (核心业务)
-- 使用事务保证ACID特性
-- ============================================
DELIMITER //

CREATE PROCEDURE sp_book_ticket(
    IN p_user_id BIGINT,
    IN p_train_schedule_id BIGINT,
    IN p_passenger_name VARCHAR(50),
    IN p_passenger_id_number VARCHAR(255),
    IN p_carriage_id BIGINT,
    IN p_seat_id BIGINT,
    IN p_from_station_id BIGINT,
    IN p_to_station_id BIGINT,
    IN p_ticket_price DECIMAL(10,2),
    IN p_seat_type VARCHAR(30),
    IN p_schedule_date DATE,
    OUT p_result_code INT,       -- 0成功, 1无票, 2冲突, 3其他错误
    OUT p_result_msg VARCHAR(200),
    OUT p_order_id BIGINT
)
BEGIN
    DECLARE v_from_order INT DEFAULT 0;
    DECLARE v_to_order INT DEFAULT 0;
    DECLARE v_order_number VARCHAR(32);
    DECLARE v_inventory_count INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result_code = 3;
        SET p_result_msg = CONCAT('系统错误: ', '数据库操作异常');
        SET p_order_id = 0;
    END;

    START TRANSACTION;

    -- 获取起止站序号
    SELECT station_order INTO v_from_order
    FROM train_route tr
    JOIN train_schedule ts ON tr.train_id = ts.train_id
    WHERE ts.id = p_train_schedule_id AND tr.station_id = p_from_station_id;

    SELECT station_order INTO v_to_order
    FROM train_route tr
    JOIN train_schedule ts ON tr.train_id = ts.train_id
    WHERE ts.id = p_train_schedule_id AND tr.station_id = p_to_station_id;

    -- 检查该座位在指定区间是否可用
    SELECT COUNT(*) INTO v_inventory_count
    FROM seat_inventory
    WHERE train_schedule_id = p_train_schedule_id
      AND seat_id = p_seat_id
      AND is_occupied = 1
      AND (
          (from_station_order < v_to_order AND to_station_order > v_from_order)
      );

    IF v_inventory_count > 0 THEN
        ROLLBACK;
        SET p_result_code = 1;
        SET p_result_msg = '该座位在指定区间已被售出';
        SET p_order_id = 0;
    ELSE
        -- 生成订单号
        SET v_order_number = CONCAT('TK', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), LPAD(FLOOR(RAND() * 10000), 4, '0'));

        -- 直接插入合并后的 ticket_order (一行=一张票)
        INSERT INTO ticket_order (order_number, user_id, train_schedule_id,
            passenger_name, passenger_id_number, seat_id, carriage_id,
            from_station_id, to_station_id, ticket_price, seat_type,
            ticket_status, payment_time, schedule_date)
        VALUES (v_order_number, p_user_id, p_train_schedule_id,
            p_passenger_name, p_passenger_id_number, p_seat_id, p_carriage_id,
            p_from_station_id, p_to_station_id, p_ticket_price, p_seat_type,
            'VALID', NOW(), p_schedule_date);

        SET p_order_id = LAST_INSERT_ID();

        -- 插入座位库存占用记录
        INSERT INTO seat_inventory (train_schedule_id, carriage_id, seat_id, from_station_order, to_station_order, is_occupied, order_id, schedule_date)
        VALUES (p_train_schedule_id, p_carriage_id, p_seat_id, v_from_order, v_to_order, 1, p_order_id, p_schedule_date);

        COMMIT;
        SET p_result_code = 0;
        SET p_result_msg = CONCAT('购票成功，订单号: ', v_order_number);
    END IF;
END //

-- ============================================
-- 2. 退票存储过程
-- ============================================
CREATE PROCEDURE sp_refund_ticket(
    IN p_ticket_id BIGINT,
    IN p_refund_reason VARCHAR(500),
    OUT p_result_code INT,
    OUT p_result_msg VARCHAR(200),
    OUT p_refund_amount DECIMAL(10,2)
)
BEGIN
    DECLARE v_ticket_status VARCHAR(20);
    DECLARE v_ticket_price DECIMAL(10,2);
    DECLARE v_schedule_date DATE;
    DECLARE v_refund_fee DECIMAL(10,2) DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result_code = 3;
        SET p_result_msg = '系统错误';
        SET p_refund_amount = 0;
    END;

    START TRANSACTION;

    SELECT ticket_status, ticket_price, schedule_date
    INTO v_ticket_status, v_ticket_price, v_schedule_date
    FROM ticket_order WHERE id = p_ticket_id;

    IF v_ticket_status IS NULL THEN
        ROLLBACK;
        SET p_result_code = 2;
        SET p_result_msg = '车票不存在';
        SET p_refund_amount = 0;
    ELSEIF v_ticket_status IN ('REFUNDED', 'CANCELLED') THEN
        ROLLBACK;
        SET p_result_code = 4;
        SET p_result_msg = '该票已退票或已取消';
        SET p_refund_amount = 0;
    ELSE
        -- 计算退票手续费 (距离出发时间越近,手续费越高)
        IF DATEDIFF(v_schedule_date, CURDATE()) > 15 THEN
            SET v_refund_fee = 0;
        ELSEIF DATEDIFF(v_schedule_date, CURDATE()) > 3 THEN
            SET v_refund_fee = ROUND(v_ticket_price * 0.05, 2);
        ELSEIF DATEDIFF(v_schedule_date, CURDATE()) > 1 THEN
            SET v_refund_fee = ROUND(v_ticket_price * 0.10, 2);
        ELSE
            SET v_refund_fee = ROUND(v_ticket_price * 0.20, 2);
        END IF;

        SET p_refund_amount = v_ticket_price - v_refund_fee;

        -- 更新票状态
        UPDATE ticket_order SET ticket_status = 'REFUNDED' WHERE id = p_ticket_id;

        -- 释放座位库存
        UPDATE seat_inventory SET is_occupied = 0, order_id = NULL WHERE order_id = p_ticket_id;

        -- 记录退票
        INSERT INTO refund_record (ticket_id, refund_amount, refund_fee, refund_reason)
        VALUES (p_ticket_id, p_refund_amount, v_refund_fee, p_refund_reason);

        COMMIT;
        SET p_result_code = 0;
        SET p_result_msg = CONCAT('退票成功，退款金额: ', p_refund_amount);
    END IF;
END //

-- ============================================
-- 3. 查询可用座位存储过程
-- 查询指定车次两站之间的所有可用座位
-- ============================================
CREATE PROCEDURE sp_find_available_seats(
    IN p_train_schedule_id BIGINT,
    IN p_from_station_id BIGINT,
    IN p_to_station_id BIGINT
)
BEGIN
    DECLARE v_from_order INT;
    DECLARE v_to_order INT;

    -- 获取起止站序号
    SELECT MIN(tr1.station_order) INTO v_from_order
    FROM train_route tr1
    JOIN train_schedule ts ON tr1.train_id = ts.train_id
    WHERE ts.id = p_train_schedule_id AND tr1.station_id = p_from_station_id;

    SELECT MIN(tr2.station_order) INTO v_to_order
    FROM train_route tr2
    JOIN train_schedule ts ON tr2.train_id = ts.train_id
    WHERE ts.id = p_train_schedule_id AND tr2.station_id = p_to_station_id;

    -- 查询在该区间未被占用的座位
    SELECT
        s.id AS seat_id,
        s.seat_number,
        s.seat_type,
        c.carriage_number,
        c.carriage_type,
        c.id AS carriage_id
    FROM seat s
    JOIN carriage c ON s.carriage_id = c.id
    JOIN train_schedule ts ON ts.train_id = c.train_id
    WHERE ts.id = p_train_schedule_id
      AND s.status = 1
      AND s.id NOT IN (
          SELECT si.seat_id
          FROM seat_inventory si
          WHERE si.train_schedule_id = p_train_schedule_id
            AND si.is_occupied = 1
            AND si.from_station_order < v_to_order
            AND si.to_station_order > v_from_order
      )
    ORDER BY c.carriage_number, s.seat_number;
END //

-- ============================================
-- 4. 查询用户订单存储过程
-- ============================================
CREATE PROCEDURE sp_get_user_orders(
    IN p_user_id BIGINT,
    IN p_page INT,
    IN p_page_size INT
)
BEGIN
    DECLARE v_offset INT;
    SET v_offset = (p_page - 1) * p_page_size;

    SELECT
        tro.id AS id,
        tro.order_number,
        tro.ticket_status,
        tro.ticket_price,
        tro.seat_type,
        tro.passenger_name,
        tro.created_at,
        ts.schedule_date,
        t.train_number,
        t.train_type,
        fs.station_name AS from_station,
        ts2.station_name AS to_station,
        c.carriage_number,
        s.seat_number
    FROM ticket_order tro
    JOIN train_schedule ts ON tro.train_schedule_id = ts.id
    JOIN train t ON ts.train_id = t.id
    JOIN station fs ON tro.from_station_id = fs.id
    JOIN station ts2 ON tro.to_station_id = ts2.id
    JOIN carriage c ON tro.carriage_id = c.id
    JOIN seat s ON tro.seat_id = s.id
    WHERE tro.user_id = p_user_id
    ORDER BY tro.created_at DESC
    LIMIT p_page_size OFFSET v_offset;
END //

-- ============================================
-- 5. 车次查询存储过程 (支持多条件)
-- ============================================
CREATE PROCEDURE sp_search_trains(
    IN p_from_station_id BIGINT,
    IN p_to_station_id BIGINT,
    IN p_date DATE,
    IN p_train_type VARCHAR(10)
)
BEGIN
    SELECT DISTINCT
        t.id AS train_id,
        t.train_number,
        t.train_type,
        t.train_name,
        ts.id AS schedule_id,
        ts.schedule_date,
        ts.available_seats,
        ts.total_seats,
        fr.departure_time,
        tr2.arrival_time,
        fs.station_name AS from_station_name,
        ts2.station_name AS to_station_name
    FROM train t
    JOIN train_schedule ts ON t.id = ts.train_id
    JOIN train_route fr ON t.id = fr.train_id AND fr.station_id = p_from_station_id
    JOIN train_route tr2 ON t.id = tr2.train_id AND tr2.station_id = p_to_station_id
    JOIN station fs ON fr.station_id = fs.id
    JOIN station ts2 ON tr2.station_id = ts2.id
    WHERE ts.schedule_date = p_date
      AND ts.status IN ('SCHEDULED', 'RUNNING')
      AND fr.station_order < tr2.station_order
      AND (p_train_type IS NULL OR p_train_type = '' OR t.train_type = p_train_type)
    ORDER BY fr.departure_time;
END //


DELIMITER ;
