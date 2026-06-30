package com.myproject.entity;

import lombok.Data;

/**
 * 可用座位查询结果 — sp_find_available_seats 的返回值
 * 不对应单张数据库表，是 seat + carriage 的聚合
 */
@Data
public class SeatInfo {
    private Long seatId;
    private String seatNumber;
    private String seatType;       // WINDOW/AISLE/MIDDLE
    private Integer carriageNumber;
    private String carriageType;   // SECOND/FIRST/BUSINESS...
    private Long carriageId;
}
