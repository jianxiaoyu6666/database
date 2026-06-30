package com.myproject.service.impl;

import com.myproject.service.WindowStatisticsService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class WindowStatisticsServiceImpl implements WindowStatisticsService {
    private final JdbcTemplate jdbcTemplate;

    public WindowStatisticsServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> routeRanking(Integer limit) {
        int safeLimit = sanitizeLimit(limit);
        String sql = """
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
                ORDER BY rank_no, total_amount DESC
                LIMIT ?
                """;
        return jdbcTemplate.queryForList(sql, safeLimit);
    }

    @Override
    public List<Map<String, Object>> userConsumptionRanking(Integer limit) {
        int safeLimit = sanitizeLimit(limit);
        String sql = """
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
                ORDER BY rank_no
                LIMIT ?
                """;
        return jdbcTemplate.queryForList(sql, safeLimit);
    }

    @Override
    public List<Map<String, Object>> trainTrend(String trainNumber, Integer days) {
        int safeDays = days == null || days < 1 || days > 365 ? 30 : days;
        String sql = """
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
                    WHERE (? IS NULL OR ? = '' OR t.train_number = ?)
                      AND s.schedule_date >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
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
                ORDER BY train_number, schedule_date
                """;
        return jdbcTemplate.queryForList(sql, trainNumber, trainNumber, trainNumber, safeDays);
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 10;
        }
        return Math.min(limit, 100);
    }
}
