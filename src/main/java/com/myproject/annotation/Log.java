package com.myproject.annotation;

import com.myproject.entity.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
    String TableName();
    OperationType operationType();
    String recordIdParam() default "";     // 参数名，谁是被操作记录的ID
}
