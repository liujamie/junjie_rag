package com.junjie.rag.aop;

import com.junjie.rag.annotation.Loggable;
import com.junjie.rag.entity.LogInfo;
import com.junjie.rag.service.impl.AsyncLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
public class LoggingAspect {

    @Autowired
    private AsyncLogService asyncLogService;

    @Pointcut("@annotation(loggable)")
    public void loggableMethods(Loggable loggable) {
    }

    @Around(value = "loggableMethods(loggable)", argNames = "joinPoint,loggable")
    public Object logAround(ProceedingJoinPoint joinPoint, Loggable loggable) throws Throwable {
        long startTime = System.currentTimeMillis();

        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            MDC.put("traceId", traceId);
        }

        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            LogInfo logInfo = new LogInfo();
            logInfo.setTraceId(traceId);

            String userIdStr = MDC.get("userId");
            if (userIdStr != null) {
                logInfo.setUserId(Long.valueOf(userIdStr));
            }

            logInfo.setMethodName(joinPoint.getSignature().getName());
            logInfo.setClassName(joinPoint.getTarget().getClass().getName());
            logInfo.setRequestTime(new Date(startTime));
            logInfo.setDuration(duration);
            logInfo.setStatus(error == null ? "success" : "fail");

            // 记录请求参数
            Object[] args = joinPoint.getArgs();
            if (loggable.value() != null && loggable.value().length() > 0) {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                String[] parameterNames = signature.getParameterNames();
                Map<String, Object> selectedParams = new HashMap<>();
                List<String> targetParams = Arrays.asList(loggable.value());
                if (parameterNames != null) {
                    for (int i = 0; i < parameterNames.length; i++) {
                        if (targetParams.contains(parameterNames[i])) {
                            selectedParams.put(parameterNames[i], args[i]);
                        }
                    }
                }
                logInfo.setRequestParams(selectedParams.toString());
            } else if (args.length > 0) {
                logInfo.setRequestParams(args.length > 5
                        ? "[" + args.length + " params]"
                        : Arrays.toString(args));
            }

            // 记录响应摘要
            if (error != null) {
                logInfo.setErrorMessage(error.getClass().getSimpleName() + ": " + error.getMessage());
            } else if (result != null) {
                String responseStr = result.toString();
                logInfo.setResponse(responseStr.length() > 1000 ? responseStr.substring(0, 1000) + "..." : responseStr);
            }

            asyncLogService.saveLog(logInfo);
        }
    }
}
