package com.junjie.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.junjie.rag.entity.Conversation;
import com.junjie.rag.mapper.ConversationMapper;
import com.junjie.rag.service.ConversationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class ConversationServiceImpl implements ConversationService {

    @Autowired
    private ConversationMapper conversationMapper;

    @Override
    public Page<Conversation> listByUserId(Long userId, int page, int size) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .eq(Conversation::getStatus, 1)
                .orderByDesc(Conversation::getUpdatedTime);
        return conversationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Conversation create(Long userId, String title) {
        Conversation conversation = Conversation.builder()
                .userId(userId)
                .title(title != null ? title : "新对话")
                .status(1)
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();
        conversationMapper.insert(conversation);
        return conversation;
    }

    @Override
    public void updateTitle(Long id, Long userId, String title) {
        conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, id)
                .eq(Conversation::getUserId, userId)
                .set(Conversation::getTitle, title)
                .set(Conversation::getUpdatedTime, LocalDateTime.now()));
    }

    @Override
    public void updateTitleIfDefault(Long id, String title) {
        int rows = conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, id)
                .eq(Conversation::getTitle, "新对话")
                .set(Conversation::getTitle, title)
                .set(Conversation::getUpdatedTime, LocalDateTime.now()));
        log.info("updateTitleIfDefault: id={}, title={}, affectedRows={}", id, title, rows);
    }

    @Override
    public void delete(Long id, Long userId) {
        int rows = conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
                .eq(Conversation::getId, id)
                .eq(Conversation::getUserId, userId)
                .set(Conversation::getStatus, 0)
                .set(Conversation::getUpdatedTime, LocalDateTime.now()));
        log.info("delete: id={}, userId={}, affectedRows={}", id, userId, rows);
    }
}
