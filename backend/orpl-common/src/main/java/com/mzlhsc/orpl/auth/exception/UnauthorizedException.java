package com.mzlhsc.orpl.auth.exception;

/**
 * 令牌缺失 / 无效 / 过期时抛出。
 * JWT 拦截器捕获后转成 401。
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}

