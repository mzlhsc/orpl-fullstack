package com.mzlhsc.orpl.common.exception;

public class BizIllegalException extends CommonException{

    public BizIllegalException(String message) {
        super(message, 422);
    }

    public BizIllegalException(String message, Throwable cause) {
        super(message, cause, 422);
    }

    public BizIllegalException(Throwable cause) {
        super(cause, 422);
    }
}
