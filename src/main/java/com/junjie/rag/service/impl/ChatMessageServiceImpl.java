package com.junjie.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.junjie.rag.entity.ChatMessage;
import com.junjie.rag.mapper.ChatMessageMapper;
import com.junjie.rag.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Override
    public List<ChatMessage> listByConversationId(Long conversationId) {
        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByAsc(ChatMessage::getId));
    }

    @Override
    public void saveMessages(List<ChatMessage> messages) {
        messages.forEach(msg -> {
            if (msg.getCreatedTime() == null) {
                msg.setCreatedTime(LocalDateTime.now());
            }
        });
        for (ChatMessage msg : messages) {
            chatMessageMapper.insert(msg);
        }
    }
}
