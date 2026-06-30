package com.myproject.entity;

import lombok.Data; 

@Data
public class Train {
    private Integer id;
    private String trainNumber; // 车次号
    private Integer stock;      // 剩余票数
}
