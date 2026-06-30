package com.myproject.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class seat_inventory {
    private Long id;
    private Long trainScheduleId;
    private Long carrigeId;
    private Long seatId;
    private Integer fromStationOrder;
    private Integer toStationOrder;
    private Integer isOccupied;
    private Long orderId;
    private LocalDateTime scheduleDate;
}
