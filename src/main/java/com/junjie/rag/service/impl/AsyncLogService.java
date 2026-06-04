package com.junjie.rag.service.impl;

import com.junjie.rag.entity.LogInfo;
import com.junjie.rag.service.LogInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AsyncLogService {

    @Autowired
    private LogInfoService logInfoService;

    @Async
    public void saveLog(LogInfo logInfo) {
        logInfoService.save(logInfo);
    }
}
