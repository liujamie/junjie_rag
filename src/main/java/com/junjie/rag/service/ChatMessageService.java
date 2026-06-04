package com.junjie.rag.service;

import com.junjie.rag.entity.ChatMessage;

import java.util.List;

public interface ChatMessageService {
    List<ChatMessage> listByConversationId(Long conversationId);
    void saveMessages(List<ChatMessage> messages);
}
