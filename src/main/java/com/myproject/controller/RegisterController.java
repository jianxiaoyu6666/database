package com.myproject.controller;

import com.myproject.entity.Result;
import com.myproject.entity.User;
import com.myproject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/register")
@RestController
public class RegisterController {
    @Autowired
    private UserService userService;

    @PostMapping
    public Result register(@RequestBody User user){
        return userService.register(user);
    }

}
