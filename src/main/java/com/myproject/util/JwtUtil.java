package com.myproject.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.HashMap;

public class JwtUtil {
    private static int expire = 3600;
    private static String key = "project";

    //生成令牌
    public static String GenerateJwt(HashMap<String,Object> data){
        String jwt  = Jwts.builder()
                .signWith(SignatureAlgorithm.HS256,key)
                .addClaims(data)
                .setExpiration(new Date(System.currentTimeMillis() + expire * 1000))
                .compact();
        return jwt;
    }

    //解析令牌
    public static Claims parseJwt(String Jwt){
        return Jwts.parser().setSigningKey(key).parseClaimsJwt(Jwt).getBody();
    }
}
