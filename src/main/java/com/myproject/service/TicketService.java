package com.myproject.service;

import com.myproject.entity.Result;
import lombok.Data;
import java.math.BigDecimal;

public interface TicketService {

    @Data
    class TicketRequest {
        private Long scheduleId;
        private Long seatId;
        private Long carriageId;
        private Long fromStationId;
        private Long toStationId;
        private String passengerName;
        private String passengerIdNumber;
        private BigDecimal ticketPrice;
        private String seatType;
        private String scheduleDate;   // yyyy-MM-dd，从查车次结果中拿
    }

    /**
     * 购票
     */
    Result buy(TicketRequest req);

    /**
     * 退票
     * @param orderId 订单ID
     * @return 退票结果
     */
    Result cancelOrder(Long orderId);

    /**
     * 查询我的订单列表
     */
    Result getMyOrders(Integer page, Integer size, Integer status);

    /**
     * 查询订单详情
     */
    Result getOrderDetail(Long orderId);
}
