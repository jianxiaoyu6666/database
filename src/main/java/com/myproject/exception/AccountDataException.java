package com.myproject.exception;

//用于处理账号数据异常信息
public class AccountDataException extends RuntimeException {
    public AccountDataException(String message) {
        super("账号数据异常:"+ message);
    }

    public AccountDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountDataException(Throwable cause) {
        super(cause);
    }
}
