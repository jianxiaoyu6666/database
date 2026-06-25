package com.myproject.service;

import com.myproject.entity.Result;
import com.myproject.entity.User;
import com.myproject.entity.logInfo;

import java.util.List;

public interface UserService {
     logInfo login(User user);

     Result register(User user);

    void update(String username, String password, String phone, String email);

    void setStatus(Integer status,Long id);

    List<User> list(User user);

    void delete(Long id);
}
