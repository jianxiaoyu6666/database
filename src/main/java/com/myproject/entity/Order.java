package com.myproject.entity;

import java.util.Date;

@Data
public class Order {
    private Integer id;
    private Integer trainId;
    private Integer userId;
    private Date createTime;
}
