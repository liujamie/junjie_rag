package com.junjie.rag.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.junjie.rag.entity.Conversation;

public interface ConversationService {
    Page<Conversation> listByUserId(Long userId, int page, int size);
    Conversation create(Long userId, String title);
    void updateTitle(Long id, Long userId, String title);
    void updateTitleIfDefault(Long id, String title);
    void delete(Long id, Long userId);
}
