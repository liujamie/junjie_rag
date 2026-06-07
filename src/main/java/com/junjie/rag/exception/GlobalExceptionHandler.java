package com.junjie.rag.exception;

import com.junjie.rag.common.BaseResponse;
import com.junjie.rag.common.ErrorCode;
import com.junjie.rag.common.ResultUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    public Object runtimeExceptionHandler(RuntimeException e, HttpServletRequest request, HttpServletResponse response) {
        // SSE 端点异步异常：响应已提交为 text/event-stream，无法再写 BaseResponse
        if ("text/event-stream".equals(response.getContentType())) {
            log.error("SSE 异步异常（已忽略）: {}", e.getMessage());
            return null;
        }
        log.error("系统异常: ", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage());
    }

}
