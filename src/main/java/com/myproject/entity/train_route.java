package com.myproject.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class train_route {
    private Long id;//线路ID
    private Long trainId;//列车ID,外键
    private Long stationId;//车站ID,外键
    private Integer stationOrder;//站点序号(从1开始)
    private LocalTime arrivalTime;//到站时间(首站为NULL)
    private LocalTime departureTime;//离站时间(末站为NULL)
    private Integer stayMinutes;//停留分钟数
    private Double distanceKm;//距起点站的距离(公里)
    private Double pricePerKm;//每公里票价系数
}
