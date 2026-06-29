package com.myproject;

import com.myproject.entity.User;
import com.myproject.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
public class testTriggerInsert {
    @Autowired
    private UserService userService;


    @Test
    void func(){
        User u = new User();
        u.setUsername("mkl1");
        u.setPassword("111");
        u.setRealName("模块");
        u.setIdNumber("11010119900101002X");
        u.setPhone("11111111113");
        u.setUserType("USER");
        userService.register(u);


    }

}
