package com.myproject.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;                    // 对应数据库表里面的用户id：主键 BIGINT
    private String username;            // 对应用户名 VARCHAR(50)
    private String password;            // 对应密码(AES加密存储) VARCHAR(255)
    private String realName;            // 对应真实姓名 VARCHAR(50)
    private String idNumber;            // 对应身份证号(AES加密存储) VARCHAR(255)
    private String phone;               // 对应手机号(AES加密存储) VARCHAR(255)
    private String email;               // 对应邮箱 VARCHAR(100)
    private String userType;            // 对应用户类型: ADMIN/STAFF/USER ENUM
    private Integer status;             // 对应状态: 1正常, 0禁用 TINYINT
    private LocalDateTime createdAt;    // 对应注册时间 DATETIME
    private LocalDateTime updatedAt;    // 对应更新时间 DATETIME
}
