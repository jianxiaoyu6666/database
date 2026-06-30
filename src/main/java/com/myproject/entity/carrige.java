package com.myproject.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class carrige {
    private Long id;//车厢ID
    private Long trainId;//列车ID
    private Integer carriageNumber;//车厢号
    private CarriageType carrageType;//车厢类型
    private Integer seatCapacity;//座位容量
    private Integer rowCount;//排数
    private LocalDateTime createAt;
}
