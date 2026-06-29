-- ============================================
-- MySQL 8 窗口函数统计示例
-- 对应接口: /api/statistics/window/*
-- ============================================

-- 热门线路排行: DENSE_RANK()
SELECT *
FROM (
    SELECT
        fs.station_name AS from_station,
        ts.station_name AS to_station,
        COUNT(*) AS ticket_count,
        COALESCE(SUM(o.ticket_price), 0) AS total_amount,
        DENSE_RANK() OVER (ORDER BY COUNT(*) DESC, COALESCE(SUM(o.ticket_price), 0) DESC) AS rank_no
    FROM ticket_order o
    JOIN station fs ON o.from_station_id = fs.id
    JOIN station ts ON o.to_station_id = ts.id
    WHERE o.ticket_status IN ('VALID', 'USED')
    GROUP BY fs.station_name, ts.station_name
) ranked_routes
ORDER BY rank_no, total_amount DESC;

-- 用户消费排行: ROW_NUMBER()
SELECT
    u.id AS user_id,
    u.username,
    COUNT(o.id) AS ticket_count,
    COALESCE(SUM(o.ticket_price), 0) AS total_amount,
    ROW_NUMBER() OVER (ORDER BY COALESCE(SUM(o.ticket_price), 0) DESC, COUNT(o.id) DESC, u.id) AS rank_no
FROM user u
LEFT JOIN ticket_order o
    ON u.id = o.user_id
   AND o.ticket_status IN ('VALID', 'USED')
GROUP BY u.id, u.username
ORDER BY rank_no;

-- 车次日趋势: SUM() OVER + LAG()
WITH daily AS (
    SELECT
        t.train_number,
        o.schedule_date,
        COUNT(o.id) AS ticket_count,
        COALESCE(SUM(o.ticket_price), 0) AS daily_amount
    FROM train t
    JOIN train_schedule s ON t.id = s.train_id
    LEFT JOIN ticket_order o
        ON s.id = o.train_schedule_id
       AND o.ticket_status IN ('VALID', 'USED')
    GROUP BY t.train_number, o.schedule_date
)
SELECT
    train_number,
    schedule_date,
    ticket_count,
    daily_amount,
    SUM(ticket_count) OVER (
        PARTITION BY train_number
        ORDER BY schedule_date
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) AS cumulative_ticket_count,
    LAG(ticket_count, 1, 0) OVER (
        PARTITION BY train_number
        ORDER BY schedule_date
    ) AS previous_ticket_count
FROM daily
ORDER BY train_number, schedule_date;
