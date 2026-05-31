package com.junjie.rag.exception;

import com.junjie.rag.common.BaseResponse;
import com.junjie.rag.common.ErrorCode;
import com.junjie.rag.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        return ResultUtils.error(e.getCode(), e.getMessage());
    }


    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("系统异常: ", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage());
    }


}