package com.myproject.controller;

import com.myproject.entity.Result;
import com.myproject.entity.User;
import com.myproject.entity.logInfo;
import com.myproject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/login")
public class loginController {
    @Autowired
    private UserService userService;

    @PostMapping
    public Result login(@RequestBody User user){
        logInfo l = userService.login(user);
        if(l == null){
            return Result.error("登录失败,请检查网络或者信息");
        }
        return Result.success(l);
    }

}
