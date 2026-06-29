package com.myproject.AOP;

import com.myproject.mapper.LogMapper;
import com.myproject.util.CurreetHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;


//这个类用于记录日志
// 与Log接口相互搭配使用
@Slf4j
@Aspect
@Component
public class LogAspect {
    @Autowired
    private LogMapper logMapper;

    //整体为一个切面Aspect
    //切入点（可以简单理解为注解部分为切入点PointCut，下面的方法为一个通知Advice）
    //调用LogMapper里面的Insert方法插入一条日志
    @Around("@annotation(com.myproject.annotation.Log)")
    public Object audit(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Long operatorId = CurreetHolder.getCurrentId();
        if(CurreetHolder.getCurrentId() != null){
            //这里该怎么写？
            logMapper.setAuditOperator(operatorId);
        }
        return proceedingJoinPoint.proceed();
    }

}
