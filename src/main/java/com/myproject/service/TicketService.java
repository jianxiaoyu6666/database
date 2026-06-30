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

    Result buy(TicketRequest req);
}
