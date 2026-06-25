package com.myproject.filter;

import com.myproject.util.CurreetHolder;
import com.myproject.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

//登录校验
@Slf4j
@WebFilter(urlPatterns = "/*")
public class TokenFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException, IOException{
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        //1.获取请求路径格式为：/login
        String URI = String.valueOf(request.getRequestURI());
        //2.判断是否是登录请求,如果url中有/login是登录请求->放行
        if(URI.contains("/login") || URI.contains("/register")){
            filterChain.doFilter(request,response);
            log.info("登录或注册请求，放行");
            return;
        }

        //3.获取请求头中的jwt令牌(也叫token)
        String token = request.getHeader("token");
        //4.判断这个token是否存在，不存在说明没有登录，返回401
        if(token == null || token.isEmpty()){
            response.setStatus(401);
            return;
        }

        //5.如果存在，校验令牌，校验失败则返回401
        try{
            Claims claims = JwtUtil.parseJwt(token);
            Long id = (Long) claims.get("id");
            String userType = (String)claims.get("userType");
            CurreetHolder.setCurrentId(id);
            CurreetHolder.setCurrentUsertype(userType);
            log.info("当前登录用户的ID为{},用户类型: {} 存入ThreadLocal",id,userType);

        }
        catch (Exception e){
            log.info("令牌非法,401");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        //6.校验通过->放行
        //这行代码的意思是：
        // TokenFilter的工作做完了，把请求交给下一个过滤器或Servlet处理
        // 底层执行：
        // 1. FilterChain 检查是否还有下一个过滤器
        // 2. 如果有 → 调用下一个过滤器的 doFilter 方法
        // 3. 如果没有 → 调用目标 Controller 的方法
        filterChain.doFilter(request,response);

        //7.删除ThreadLocal的数据
        CurreetHolder.remove();
    }

}
