package com.myproject.controller;

import com.myproject.entity.Result;
import com.myproject.entity.User;
import com.myproject.service.UserService;

import com.myproject.util.CurreetHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    //前端设计的时候要直接向post/update/{id}发出请求，至于前端的这个id如何获得，可以再前端用jwt解析得到
    @PostMapping("/update/{id}")
    public Result update(@PathVariable Long id,
                         @RequestParam(required = false) String username,
                         @RequestParam(required = false) String password,
                         @RequestParam(required = false) String phone,
                         @RequestParam(required = false) String email){
        if(!CurreetHolder.getCurrentId().equals(id)){
            return Result.error("无权修改！");
        }
        userService.update(username,password,phone,email);
        return Result.success("信息更新成功！");
    }

    @PostMapping("/setStatus/{id}")
    public Result setStatus(@PathVariable Long id,
                            @RequestParam Integer status){
        String currentUserType = CurreetHolder.getCurrentUserType();
        Long currentUserId = CurreetHolder.getCurrentId();

        if(!"ADMIN".equals(currentUserType)){
            return Result.error("只有管理员可以修改用户账号状态");
        }

        if(id.equals(currentUserId)){
            return Result.error("不能修改自己的状态");
        }

        userService.setStatus(status,id);
        return Result.success("修改状态成功！");
    }

    //查询指定条件用户（包括所有用户），此功能只有管理员可以使用
    @PostMapping("/list")
    public Result list(@RequestBody User user){
        String currentUserType = CurreetHolder.getCurrentUserType();
        if(!"ADMIN".equals(currentUserType)){
            return Result.error("权限不足");
        }
        List<User> userList = userService.list(user);
        return Result.success(userList);
    }

    //注销账号
    @DeleteMapping("/delete")
    public Result delete(Long id){
        if(!CurreetHolder.getCurrentId().equals(id) && !CurreetHolder.getCurrentUserType().equals("ADMIN")){
            return Result.error("你没有权限！");
        }
        userService.delete(id);
        return Result.success("注销成功！");
    }
}
