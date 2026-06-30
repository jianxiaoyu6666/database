package com.myproject.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class train_schedule {
    private Long id;//排班ID
    private Long trainId;//列车ID
    private LocalDate scheduleDate;//运营日期
    private String status;//状态: SCHEDULED已计划, RUNNING运行中, CANCELLED已取消, COMPLETED已完成
    private Integer availableSeats;//剩余座位数(冗余字段,触发器更新)
    private Integer totalSeats;//总座位数
    private LocalDateTime createdAt;//创建时间
}
