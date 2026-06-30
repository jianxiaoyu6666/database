package com.myproject.entity;

import lombok.Data;

import java.math.BigDecimal;
//对应于存储过程sp_book_ticket的参数
@Data
public class TicketRequest {
    private Long scheduleId;        // 排班ID
    private Long seatId;            // 座位ID
    private Long carriageId;        // 车厢ID
    private Long fromStationId;     // 出发站ID
    private Long toStationId;       // 到达站ID
    private String passengerName;   // 乘客姓名
    private String passengerIdNumber;// 身份证号（明文，Service里加密）
    private BigDecimal ticketPrice; // 票价,用BigDecimal是因为钱这玩意要保留最精细的精度
    private String seatType;        // 座位类型（二等座/一等座...）
}