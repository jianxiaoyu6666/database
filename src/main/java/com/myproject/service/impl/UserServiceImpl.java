package com.myproject.service.impl;

import com.myproject.annotation.Log;
import com.myproject.entity.OperationType;
import com.myproject.entity.Result;
import com.myproject.entity.User;
import com.myproject.entity.logInfo;
import com.myproject.exception.AccountDataException;
import com.myproject.mapper.UserMapper;
import com.myproject.service.UserService;
import com.myproject.util.AESUtil;
import com.myproject.util.CurreetHolder;
import com.myproject.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;


    @Override
    public logInfo login(User user) {
        //简单校验
        if(user.getUsername() == null){
            throw new RuntimeException("登陆时用户名不能为空！");
        }

        if(user.getPassword() == null){
            throw new RuntimeException("密码不能为空！");
        }
        String inputPassword = user.getPassword();  // 保存明文密码
        User u = userMapper.findByUsername(user);
        //数据库里面没找到，登陆信息返回null
        if(u == null){
            return null;
        }
        // 解密库里存的密码，与输入密码明文比对
        String storedPassword = AESUtil.decrypt(u.getPassword());
        if (!inputPassword.equals(storedPassword)) {
            return null;
        }
        //找到了但是状态为禁用
        if(u.getStatus() == 0){
            log.info("此账号被封禁！");
            return null;
        }
        HashMap<String,Object>data = new HashMap<>();
        data.put("id",u.getId());
        data.put("username",u.getUsername());
        data.put("userType",u.getUserType());
        data.put("status",u.getStatus());
        String token = JwtUtil.GenerateJwt(data);
        return new logInfo(u.getId(),u.getUsername(),token);
    }

    @Log(TableName = "user", operationType = OperationType.INSERT)
    @Override
    public Result register(User user) {
        //基础检验，应该这样就可以了，没必要再继续写更加复杂的检验了
        if(user.getRealName() == null){
            return Result.error("用户真实姓名不可为空！");
        }
        if(user.getPassword() == null){
            return Result.error("用户密码不可为空！");
        }
        if(user.getPhone() == null){
            return Result.error("用户手机号不可为空！");
        }
        if(user.getUsername() == null){
            return Result.error("用户名不可为空！");
        }
        if(user.getIdNumber() == null || !user.getIdNumber().matches("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$")){
            return Result.error("用户身份证号不可为空并且要满18位！");
        }
        //隐私数据加密入库
        String encryptedPassword = AESUtil.encrypt(user.getPassword());
        String encryptedIdNumber = AESUtil.encrypt(user.getIdNumber());
        String encryptedPhone = AESUtil.encrypt(user.getPhone());
        user.setPhone(encryptedPhone);
        user.setIdNumber(encryptedIdNumber);
        user.setPassword(encryptedPassword);
        userMapper.save(user);
        return Result.success("注册成功");
    }

    @Override
    public void update(String username, String password, String phone, String email) {
        if(username == null || username.isEmpty()){
            throw new AccountDataException("用户名不能为空！");
        }
        if(password == null || password.isEmpty()){
            throw new AccountDataException("账号密码不能为空！");
        }

        if(phone == null || phone.isEmpty()){
            throw new AccountDataException("手机号不能为空！");
        }
        //加密
        String newPassword = AESUtil.encrypt(password);
        String newPhone = AESUtil.encrypt(phone);

        userMapper.update(CurreetHolder.getCurrentId(),username,newPassword,newPhone,email,LocalDateTime.now());
    }

    @Override
    public void setStatus(Integer status,Long id) {
        userMapper.setStatus(status,id);
    }

//  存储过程调用（就是数据库层面的函数封装）
//    -- 查用户名带"张"且类型为USER且状态为1的用户
//    CALL sp_search_user('张', NULL, 'USER', 1);
//
//-- 查姓名带"三"的所有用户（其他条件不限）
//    CALL sp_search_user(NULL, '三', NULL, NULL);
//
//-- 查所有用户（所有条件都不限）
//    CALL sp_search_user(NULL, NULL, NULL, NULL);
//存储过程的定义
//CREATE PROCEDURE 名字(参数)
//    BEGIN
//            SQL语句;
//    END
    @Override
    public List<User> list(User user) {
        return userMapper.list(user.getUsername(),
                user.getRealName(),
                user.getUserType(),
                user.getStatus());
    }

    @Override
    public void delete(Long id) {
        userMapper.deleteByID(id);
    }
}
