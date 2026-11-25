package com.mzlhsc.orpl.common.exception;

/**
 * 资源不存在异常，统一返回 404。
 */
public class NotFoundException extends CommonException {

    public NotFoundException(String message) {
        super(message, 404);
    }
}