package com.junjie.rag.aop;

import com.junjie.rag.annotation.TrackLlmCall;
import com.junjie.rag.entity.LlmCallRecord;
import com.junjie.rag.service.LlmCallRecordService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 拦截 @TrackLlmCall 标注的方法，记录 LLM 调用的耗时、token 估算等
 */
@Aspect
@Component
public class LlmCallAspect {

    @Autowired
    private LlmCallRecordService llmCallRecordService;

    @Pointcut("@annotation(track)")
    public void llmCalls(TrackLlmCall track) {
    }

    @Around(value = "llmCalls(track)", argNames = "joinPoint,track")
    public Object aroundLlmCall(ProceedingJoinPoint joinPoint, TrackLlmCall track) throws Throwable {
        long startMs = System.currentTimeMillis();
        String traceId = MDC.get("traceId");
        String userIdStr = MDC.get("userId");

        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            long durationMs = System.currentTimeMillis() - startMs;
            String serviceName = track.service();
            String modelName = track.model();

            // 尝试从方法参数提取更精确的模型名（如果标注里没写死）
            if (modelName.isEmpty()) {
                modelName = extractModelFromArgs(joinPoint.getArgs());
            }

            // 估算 token 数（粗略：1 token ≈ 2 chars）
            String requestPreview = extractRequestPreview(joinPoint);
            String responsePreview = result != null ? truncate(result.toString(), 500) : "";
            int inputTokens = estimateTokens(requestPreview);
            int outputTokens = estimateTokens(responsePreview);

            LlmCallRecord record = new LlmCallRecord();
            record.setTraceId(traceId);
            if (userIdStr != null) record.setUserId(Long.valueOf(userIdStr));
            record.setServiceName(serviceName);
            record.setModelName(modelName);
            record.setInputTokens(inputTokens);
            record.setOutputTokens(outputTokens);
            record.setDurationMs(durationMs);
            record.setStatus(error == null ? "success" : "fail");
            record.setRequestPreview(truncate(requestPreview, 500));
            record.setResponsePreview(truncate(responsePreview, 500));
            record.setCreateTime(new Date());

            try {
                llmCallRecordService.save(record);
            } catch (Exception e) {
                // 记录 LLM 调用日志失败不应影响主流程
                MDC.get("traceId");
            }
        }
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        // 粗略估算：中文约 1.5 chars/token，英文约 4 chars/token
        int en = 0, cn = 0;
        for (char c : text.toCharArray()) {
            if (c > 0x7F) cn++;
            else if (c > ' ') en++;
        }
        return (int)(cn / 1.5 + en / 4.0 + 1);
    }

    private String extractRequestPreview(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (arg == null) continue;
            String str = arg.toString();
            if (str.length() > 200) str = str.substring(0, 200) + "...";
            sb.append(str).append(" | ");
        }
        return sb.toString();
    }

    private String extractModelFromArgs(Object[] args) {
        // 某些服务在请求体 JSON 中指定 model 名称，尝试提取
        if (args == null) return "";
        for (Object arg : args) {
            if (arg instanceof String s && s.contains("\"model\"")) {
                int idx = s.indexOf("\"model\"");
                int valStart = s.indexOf('"', idx + 8);
                if (valStart > 0) {
                    int valEnd = s.indexOf('"', valStart + 1);
                    if (valEnd > valStart) return s.substring(valStart + 1, valEnd);
                }
            }
        }
        return "";
    }

    private String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}
