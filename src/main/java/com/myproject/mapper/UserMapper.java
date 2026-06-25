package com.myproject.mapper;

import com.myproject.entity.User;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {
    //sql直接执行，Options用于回填id值
    @Insert("INSERT INTO user(username, password, real_name, id_number, phone, email, user_type) " +
            "VALUES(#{username}, #{password}, #{realName}, #{idNumber}, #{phone}, #{email}, #{userType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void save(User user);

    @Select("SELECT id,username,password,user_type,status from user where username = #{username}")
    User findByUsername(User user);

    @Update("update user set " +
            "username = #{username}," +
            "password = #{password}," +
            "phone = #{phone}," +
            "email = #{email}," +
            "updated_at = #{updatedAt} " +
            "where id = #{id}")
    void update(@Param("id")Long id,
                @Param("username") String username,
                @Param("password") String password,
                @Param("phone") String phone,
                @Param("email") String email,
                @Param("updatedAt") LocalDateTime updatedAt);

    @Update("update user set status = #{status} where id = #{id}")
    void setStatus(@Param("status")Integer status, @Param("id") Long id);

    List<User> list(String username, String realName, String userType, Integer status);

    @Delete("delete from user where id = #{id}")
    void deleteByID(Long id);
}
