-- ============================================
-- 火车售票管理系统 - 数据库建表脚本
-- 数据库: train_ticket_db
--
-- 简化设计: ticket_order 合并原 order_item, 一行=一张票
-- MySQL分区表两大限制及对策:
-- 1. 分区表不支持FOREIGN KEY → 应用层校验 + 触发器补偿级联操作
-- 2. 所有UNIQUE KEY(含PRIMARY KEY)必须包含分区键 → 唯一约束中加入schedule_date字段
-- 分区表(train_schedule, seat_inventory, ticket_order, audit_log) 共4张
-- ============================================

CREATE DATABASE IF NOT EXISTS train_ticket_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE train_ticket_db;

-- ============================================
-- 1. 用户表 (user)
-- 包含数据加密: 密码使用AES加密, 身份证号加密存储
-- ============================================
CREATE TABLE `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(AES加密存储)',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `id_number` VARCHAR(255) NOT NULL COMMENT '身份证号(AES加密存储)',
    `phone` VARCHAR(255) NOT NULL COMMENT '手机号(AES加密存储)',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `user_type` ENUM('ADMIN', 'STAFF', 'USER') NOT NULL DEFAULT 'USER' COMMENT '用户类型: ADMIN管理员, STAFF车站工作人员, USER普通用户',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1正常, 0禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_user_type` (`user_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 车站表 (station)
-- 支持全文检索
-- ============================================
CREATE TABLE `station` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '车站ID',
    `station_name` VARCHAR(100) NOT NULL COMMENT '车站名称',
    `station_code` VARCHAR(10) NOT NULL COMMENT '车站代码(拼音缩写)',
    `city` VARCHAR(50) NOT NULL COMMENT '所在城市',
    `province` VARCHAR(50) NOT NULL COMMENT '所在省份',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_station_code` (`station_code`),
    KEY `idx_city` (`city`),
    FULLTEXT KEY `ft_station_name` (`station_name`, `city`, `province`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车站表';

-- ============================================
-- 3. 列车表 (train)
-- ============================================
CREATE TABLE `train` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '列车ID',
    `train_number` VARCHAR(20) NOT NULL COMMENT '车次号(如G101, D301, T110)',
    `train_type` ENUM('G', 'D', 'T', 'K', 'Z', 'C') NOT NULL COMMENT '列车类型: G高铁, D动车, T特快, K快速, Z直达, C城际',
    `train_name` VARCHAR(100) DEFAULT NULL COMMENT '列车名称',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_train_number` (`train_number`),
    KEY `idx_train_type` (`train_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='列车表';

-- ============================================
-- 4. 列车线路表 (train_route)
-- 存储列车经过的各站点及时间信息
-- ============================================
CREATE TABLE `train_route` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '线路ID',
    `train_id` BIGINT NOT NULL COMMENT '列车ID',
    `station_id` BIGINT NOT NULL COMMENT '车站ID',
    `station_order` INT NOT NULL COMMENT '站点序号(从1开始)',
    `arrival_time` TIME DEFAULT NULL COMMENT '到站时间(首站为NULL)',
    `departure_time` TIME DEFAULT NULL COMMENT '离站时间(末站为NULL)',
    `stay_minutes` INT DEFAULT 0 COMMENT '停留分钟数',
    `distance_km` DECIMAL(10,2) DEFAULT 0 COMMENT '距起点站的距离(公里)',
    `price_per_km` DECIMAL(10,4) DEFAULT 0 COMMENT '每公里票价系数',
    KEY `idx_train_id` (`train_id`),
    KEY `idx_station_id` (`station_id`),
    KEY `idx_train_station_order` (`train_id`, `station_order`),
    CONSTRAINT `fk_route_train` FOREIGN KEY (`train_id`) REFERENCES `train`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_route_station` FOREIGN KEY (`station_id`) REFERENCES `station`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='列车线路表(车次-站点关系)';

-- ============================================
-- 5. 列车排班表 (train_schedule)
-- 存储每天/每个日期的列车运营计划
-- 按月分区以支持大数据量场景
-- ============================================
CREATE TABLE `train_schedule` (
    `id` BIGINT AUTO_INCREMENT COMMENT '排班ID',
    `train_id` BIGINT NOT NULL COMMENT '列车ID',
    `schedule_date` DATE NOT NULL COMMENT '运营日期',
    `status` ENUM('SCHEDULED', 'RUNNING', 'CANCELLED', 'COMPLETED') NOT NULL DEFAULT 'SCHEDULED' COMMENT '状态',
    `available_seats` INT DEFAULT 0 COMMENT '剩余座位数(冗余字段,触发器更新)',
    `total_seats` INT DEFAULT 0 COMMENT '总座位数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`, `schedule_date`),
    KEY `idx_train_id` (`train_id`),
    KEY `idx_schedule_date` (`schedule_date`),
    KEY `idx_train_date` (`train_id`, `schedule_date`),
    KEY `idx_status` (`status`)
    -- 注意: 分区表不支持FOREIGN KEY, train_id的引用完整性由应用层+触发器保证
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='列车排班表(分区表)'
PARTITION BY RANGE (TO_DAYS(`schedule_date`)) (
    PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
    PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
    PARTITION p202503 VALUES LESS THAN (TO_DAYS('2025-04-01')),
    PARTITION p202504 VALUES LESS THAN (TO_DAYS('2025-05-01')),
    PARTITION p202505 VALUES LESS THAN (TO_DAYS('2025-06-01')),
    PARTITION p202506 VALUES LESS THAN (TO_DAYS('2025-07-01')),
    PARTITION p202507 VALUES LESS THAN (TO_DAYS('2025-08-01')),
    PARTITION p202508 VALUES LESS THAN (TO_DAYS('2025-09-01')),
    PARTITION p202509 VALUES LESS THAN (TO_DAYS('2025-10-01')),
    PARTITION p202510 VALUES LESS THAN (TO_DAYS('2025-11-01')),
    PARTITION p202511 VALUES LESS THAN (TO_DAYS('2025-12-01')),
    PARTITION p202512 VALUES LESS THAN (TO_DAYS('2026-01-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ============================================
-- 6. 车厢表 (carriage)
-- ============================================
CREATE TABLE `carriage` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '车厢ID',
    `train_id` BIGINT NOT NULL COMMENT '列车ID',
    `carriage_number` INT NOT NULL COMMENT '车厢号',
    `carriage_type` ENUM('BUSINESS', 'FIRST', 'SECOND', 'SOFT_SLEEPER', 'HARD_SLEEPER', 'SOFT_SEAT', 'HARD_SEAT', 'STANDING') NOT NULL COMMENT '车厢类型',
    `seat_capacity` INT NOT NULL DEFAULT 0 COMMENT '座位容量',
    `row_count` INT DEFAULT 0 COMMENT '排数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_train_id` (`train_id`),
    CONSTRAINT `fk_carriage_train` FOREIGN KEY (`train_id`) REFERENCES `train`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车厢表';

-- ============================================
-- 7. 座位表 (seat)
-- ============================================
CREATE TABLE `seat` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '座位ID',
    `carriage_id` BIGINT NOT NULL COMMENT '车厢ID',
    `seat_number` VARCHAR(10) NOT NULL COMMENT '座位号(如01A, 02F)',
    `seat_type` ENUM('WINDOW', 'AISLE', 'MIDDLE') DEFAULT NULL COMMENT '座位位置: 靠窗/过道/中间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0不可用,1可用',
    KEY `idx_carriage_id` (`carriage_id`),
    UNIQUE KEY `uk_carriage_seat` (`carriage_id`, `seat_number`),
    CONSTRAINT `fk_seat_carriage` FOREIGN KEY (`carriage_id`) REFERENCES `carriage`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='座位表';

-- ============================================
-- 8. 座位库存表 (seat_inventory)
-- 按日期+车次+座位+区间管理库存, 支持精确的区间售票控制
-- 按月分区支持大数据量
-- ============================================
CREATE TABLE `seat_inventory` (
    `id` BIGINT AUTO_INCREMENT COMMENT '库存ID',
    `train_schedule_id` BIGINT NOT NULL COMMENT '排班ID',
    `carriage_id` BIGINT NOT NULL COMMENT '车厢ID',
    `seat_id` BIGINT NOT NULL COMMENT '座位ID',
    `from_station_order` INT NOT NULL COMMENT '起始站序号(在train_route中)',
    `to_station_order` INT NOT NULL COMMENT '到达站序号(在train_route中)',
    `is_occupied` TINYINT NOT NULL DEFAULT 0 COMMENT '是否被占用: 0未占用,1已占用',
    `order_id` BIGINT DEFAULT NULL COMMENT '关联ticket_order.id',
    `schedule_date` DATE NOT NULL COMMENT '运营日期(用于分区)',
    PRIMARY KEY (`id`, `schedule_date`),
    KEY `idx_schedule_id` (`train_schedule_id`),
    KEY `idx_seat_id` (`seat_id`),
    KEY `idx_schedule_seat` (`train_schedule_id`, `seat_id`),
    KEY `idx_occupied` (`train_schedule_id`, `from_station_order`, `to_station_order`, `is_occupied`)
    -- 注意: 分区表不支持FOREIGN KEY, 引用完整性由应用层+触发器保证
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='座位库存表(区间占用管理)'
PARTITION BY RANGE (TO_DAYS(`schedule_date`)) (
    PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
    PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
    PARTITION p202503 VALUES LESS THAN (TO_DAYS('2025-04-01')),
    PARTITION p202504 VALUES LESS THAN (TO_DAYS('2025-05-01')),
    PARTITION p202505 VALUES LESS THAN (TO_DAYS('2025-06-01')),
    PARTITION p202506 VALUES LESS THAN (TO_DAYS('2025-07-01')),
    PARTITION p202507 VALUES LESS THAN (TO_DAYS('2025-08-01')),
    PARTITION p202508 VALUES LESS THAN (TO_DAYS('2025-09-01')),
    PARTITION p202509 VALUES LESS THAN (TO_DAYS('2025-10-01')),
    PARTITION p202510 VALUES LESS THAN (TO_DAYS('2025-11-01')),
    PARTITION p202511 VALUES LESS THAN (TO_DAYS('2025-12-01')),
    PARTITION p202512 VALUES LESS THAN (TO_DAYS('2026-01-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ============================================
-- 9. 火车票订单表 (ticket_order) — 合并了原 order_item
-- 每行 = 一张票, 同一 order_number 的多行为同一订单的多张票
-- 按月分区
-- ============================================
CREATE TABLE `ticket_order` (
    `id` BIGINT AUTO_INCREMENT COMMENT '车票ID',
    `order_number` VARCHAR(32) NOT NULL COMMENT '订单号(同一订单多张票共享)',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `train_schedule_id` BIGINT NOT NULL COMMENT '排班ID',
    `passenger_name` VARCHAR(50) NOT NULL COMMENT '乘客姓名',
    `passenger_id_number` VARCHAR(255) NOT NULL COMMENT '乘客身份证号(AES加密)',
    `seat_id` BIGINT NOT NULL COMMENT '座位ID',
    `carriage_id` BIGINT NOT NULL COMMENT '车厢ID',
    `from_station_id` BIGINT NOT NULL COMMENT '出发站ID',
    `to_station_id` BIGINT NOT NULL COMMENT '到达站ID',
    `ticket_price` DECIMAL(10,2) NOT NULL COMMENT '票价',
    `seat_type` VARCHAR(30) NOT NULL COMMENT '座位类型(商务座/一等座/二等座等)',
    `ticket_status` ENUM('VALID','CANCELLED','REFUNDED','USED') NOT NULL DEFAULT 'VALID' COMMENT '车票状态',
    `payment_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `schedule_date` DATE NOT NULL COMMENT '出发日期(用于分区)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`, `schedule_date`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_schedule_id` (`train_schedule_id`),
    KEY `idx_seat_id` (`seat_id`),
    KEY `idx_ticket_status` (`ticket_status`),
    KEY `idx_passenger_name` (`passenger_name`),
    KEY `idx_from_to_station` (`from_station_id`, `to_station_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_order_number` (`order_number`)
    -- 注意: 分区表不支持FOREIGN KEY, user_id/train_schedule_id/seat_id的引用完整性由应用层+触发器保证
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='火车票订单表(合并订单+明细)'
PARTITION BY RANGE (TO_DAYS(`schedule_date`)) (
    PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
    PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
    PARTITION p202503 VALUES LESS THAN (TO_DAYS('2025-04-01')),
    PARTITION p202504 VALUES LESS THAN (TO_DAYS('2025-05-01')),
    PARTITION p202505 VALUES LESS THAN (TO_DAYS('2025-06-01')),
    PARTITION p202506 VALUES LESS THAN (TO_DAYS('2025-07-01')),
    PARTITION p202507 VALUES LESS THAN (TO_DAYS('2025-08-01')),
    PARTITION p202508 VALUES LESS THAN (TO_DAYS('2025-09-01')),
    PARTITION p202509 VALUES LESS THAN (TO_DAYS('2025-10-01')),
    PARTITION p202510 VALUES LESS THAN (TO_DAYS('2025-11-01')),
    PARTITION p202511 VALUES LESS THAN (TO_DAYS('2025-12-01')),
    PARTITION p202512 VALUES LESS THAN (TO_DAYS('2026-01-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ============================================
-- 11. 常用乘客表 (passenger)
-- ============================================
CREATE TABLE `passenger` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '乘客ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `passenger_name` VARCHAR(50) NOT NULL COMMENT '乘客姓名',
    `passenger_id_number` VARCHAR(255) NOT NULL COMMENT '身份证号(AES加密)',
    `passenger_type` ENUM('ADULT', 'STUDENT', 'CHILD') NOT NULL DEFAULT 'ADULT' COMMENT '乘客类型',
    `phone` VARCHAR(255) DEFAULT NULL COMMENT '联系电话(AES加密存储)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_passenger_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='常用乘客表';

-- ============================================
-- 12. 审计日志表 (audit_log)
-- 按月分区, 记录所有关键数据变更
-- ============================================
CREATE TABLE `audit_log` (
    `id` BIGINT AUTO_INCREMENT COMMENT '日志ID',
    `table_name` VARCHAR(50) NOT NULL COMMENT '操作表名',
    `operation_type` ENUM('INSERT', 'UPDATE', 'DELETE') NOT NULL COMMENT '操作类型',
    `record_id` BIGINT DEFAULT NULL COMMENT '记录ID',
    `old_value` TEXT DEFAULT NULL COMMENT '旧值(JSON)',
    `new_value` TEXT DEFAULT NULL COMMENT '新值(JSON)',
    `operated_by` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `log_date` DATE NOT NULL COMMENT '日志日期(用于分区)',
    PRIMARY KEY (`id`, `log_date`),
    KEY `idx_table_name` (`table_name`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_operated_by` (`operated_by`),
    KEY `idx_operated_at` (`operated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表(分区表, 按月分区)'
PARTITION BY RANGE (TO_DAYS(`log_date`)) (
    PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
    PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
    PARTITION p202503 VALUES LESS THAN (TO_DAYS('2025-04-01')),
    PARTITION p202504 VALUES LESS THAN (TO_DAYS('2025-05-01')),
    PARTITION p202505 VALUES LESS THAN (TO_DAYS('2025-06-01')),
    PARTITION p202506 VALUES LESS THAN (TO_DAYS('2025-07-01')),
    PARTITION p202507 VALUES LESS THAN (TO_DAYS('2025-08-01')),
    PARTITION p202508 VALUES LESS THAN (TO_DAYS('2025-09-01')),
    PARTITION p202509 VALUES LESS THAN (TO_DAYS('2025-10-01')),
    PARTITION p202510 VALUES LESS THAN (TO_DAYS('2025-11-01')),
    PARTITION p202511 VALUES LESS THAN (TO_DAYS('2025-12-01')),
    PARTITION p202512 VALUES LESS THAN (TO_DAYS('2026-01-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ============================================
-- 13. 退票记录表 (refund_record)
-- ============================================
CREATE TABLE `refund_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '退票ID',
    `ticket_id` BIGINT NOT NULL COMMENT '关联 ticket_order.id',
    `refund_amount` DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    `refund_fee` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '退票手续费',
    `refund_reason` VARCHAR(500) DEFAULT NULL COMMENT '退票原因',
    `refund_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '退票时间',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    KEY `idx_ticket_id` (`ticket_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退票记录表';
