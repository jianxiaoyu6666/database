package com.myproject.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TrainSearchInfo {
    // 列名 train_id → 字段名 trainId（驼峰自动映射）
    private Long trainId;          // SP 列: train_id
    private String trainNumber;    // train_number
    private String trainType;      // train_type
    private String trainName;      // train_name
    private Long scheduleId;       // schedule_id
    private LocalDate scheduleDate;// schedule_date
    private LocalTime departureTime; // departure_time ← TIME 字段用 LocalTime
    private LocalTime arrivalTime;   // arrival_time
    private String fromStationName;  // from_station_name
    private String toStationName;    // to_station_name
    private Integer availableSeats;  // available_seats
    private Integer totalSeats;      // total_seats
}
