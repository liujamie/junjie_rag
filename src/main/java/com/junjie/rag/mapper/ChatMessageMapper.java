package com.junjie.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.junjie.rag.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
