package com.myproject.exception;

import com.myproject.entity.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@ResponseBody
@ControllerAdvice
//抓取全局异常信息（这个是保证后端与前端交互格式为{code,msg,data}最为重要的地方）
public class GlobalExceptionHandler {
    @ExceptionHandler
    public Result handleException(Exception e){
        log.info("出错了",e);
        return Result.error(e.getMessage());
    }
}
