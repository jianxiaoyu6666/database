package com.myproject.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

//数据变动的日志记录我还没写，
// 但是按照建表的sql，基本可以确定只要有数据的更新就要存一条日志
//比如说，用户修改了自己的信息，要存一条；用户注销账号，要存一条。
// 车票变动也要存

@Data
@AllArgsConstructor
@NoArgsConstructor
public class audit_log {
    private Long id;
    private String tableName;
    private OperationType operationType;
    private Long recordId;              //
    private String oldValue;
    private String newValue;
    private Long operatedBy;            //操作人ID
    private LocalDateTime operatedAt;   //操作时间
    private LocalDate logDate;          //
}

