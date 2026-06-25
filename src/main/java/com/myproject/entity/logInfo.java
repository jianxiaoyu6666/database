package com.myproject.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
//登录信息
public class logInfo {
    private Long id;
    private String username;
    private String token;
}
