package com.myproject.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Train {
    private Long id;//列车ID
    private String trainNumber;//车次号(如G101, D301, T110)
    private String trainType;//列车类型: G高铁, D动车, T特快, K快速, Z直达, C城际
    private String trainName;//列车名称
    private LocalDateTime createdAt;//创建时间
}
