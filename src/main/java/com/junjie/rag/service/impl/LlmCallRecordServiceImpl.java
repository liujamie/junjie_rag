package com.junjie.rag.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.junjie.rag.entity.LlmCallRecord;
import com.junjie.rag.mapper.LlmCallRecordMapper;
import com.junjie.rag.service.LlmCallRecordService;
import org.springframework.stereotype.Service;

@Service
public class LlmCallRecordServiceImpl extends ServiceImpl<LlmCallRecordMapper, LlmCallRecord>
        implements LlmCallRecordService {
}
