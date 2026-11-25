package com.mzlhsc.orpl.common.advice;

import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.common.exception.CommonException;
import com.mzlhsc.orpl.common.exception.DbException;
import com.mzlhsc.orpl.common.utils.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.mzlhsc.orpl.common.result.R;

import java.util.stream.Collectors;

/**
 * 状态码设置
 */

@RestControllerAdvice
@Slf4j
public class CommonExceptionAdvice {

    @ExceptionHandler(DbException.class)
    public Object handleDbException(DbException e) {
        log.error("mysql数据库操作异常 -> ", e);
        return processResponse(e);
    }

    @ExceptionHandler(CommonException.class)
    public Object handleBadRequestException(CommonException e) {
        log.error("自定义异常 -> {} , 异常原因：{}  ",e.getClass().getName(), e.getMessage());
        log.debug("", e);
        return processResponse(e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors()
                .stream().map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining("|"));
        log.error("请求参数校验异常 -> {}", msg);
        log.debug("", e);
        return processResponse(new BadRequestException(msg));
    }
    @ExceptionHandler(BindException.class)
    public Object handleBindException(BindException e) {
        log.error("请求参数绑定异常 ->BindException， {}", e.getMessage());
        log.debug("", e);
        return processResponse(new BadRequestException("请求参数格式错误"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Object handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不正确 -> {}={}", e.getName(), e.getValue());
        return processResponse(new CommonException("参数类型不正确", 400));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Object handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数 -> {}", e.getParameterName());
        return processResponse(new CommonException("缺少请求参数：" + e.getParameterName(), 400));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式不正确 -> {}", e.getMessage());
        return processResponse(new CommonException("请求体格式不正确", 400));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFound(NoResourceFoundException e) {
        log.warn("接口不存在 -> {}", e.getResourcePath());
        return processResponse(new CommonException("接口不存在", 404));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持 -> {}", e.getMessage());
        return processResponse(new CommonException("请求方法不支持", 405));
    }

    @ExceptionHandler(Exception.class)
    public Object handleRuntimeException(Exception e) {
        String uri = "unknown";
        try {
            if (WebUtils.getRequest() != null) {
                uri = WebUtils.getRequest().getRequestURI();
            }
        } catch (Exception ignored) {
            // 非 Web 线程（如异步线程）取不到请求信息，忽略即可
        }
        log.error("其他异常 uri : {} -> ", uri, e);
        return processResponse(new CommonException("服务器内部异常", 500));
    }

    private ResponseEntity<R<Void>> processResponse(CommonException e){
        return ResponseEntity.status(e.getCode()).body(R.error(e));
    }
}
