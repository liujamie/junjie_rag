package com.junjie.rag.controller;

import com.junjie.rag.common.ApplicationConstant;
import com.junjie.rag.common.BaseResponse;
import com.junjie.rag.common.ResultUtils;
import com.junjie.rag.context.BaseContext;
import com.junjie.rag.entity.ChatMessage;
import com.junjie.rag.entity.Conversation;
import com.junjie.rag.service.ChatMessageService;
import com.junjie.rag.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "ConversationController", description = "会话历史管理")
@Slf4j
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/chat")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Operation(summary = "会话列表")
    @GetMapping("/conversations")
    public BaseResponse<?> listConversations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = BaseContext.getCurrentId();
        return ResultUtils.success(conversationService.listByUserId(userId, page, size));
    }

    @Operation(summary = "新建会话")
    @PostMapping("/conversation")
    public BaseResponse<?> createConversation(@RequestBody(required = false) Map<String, String> body) {
        Long userId = BaseContext.getCurrentId();
        String title = body != null ? body.get("title") : null;
        Conversation conversation = conversationService.create(userId, title);
        return ResultUtils.success(conversation);
    }

    @Operation(summary = "更新会话标题")
    @PutMapping("/conversation/{id}/title")
    public BaseResponse<?> updateTitle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = BaseContext.getCurrentId();
        conversationService.updateTitle(id, userId, body.get("title"));
        return ResultUtils.success(null);
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/conversation/{id}")
    public BaseResponse<?> deleteConversation(@PathVariable Long id) {
        Long userId = BaseContext.getCurrentId();
        log.info("删除会话: id={}, userId={}", id, userId);
        conversationService.delete(id, userId);
        return ResultUtils.success(null);
    }

    @Operation(summary = "会话消息列表")
    @GetMapping("/conversation/{id}/messages")
    public BaseResponse<?> listMessages(@PathVariable Long id) {
        return ResultUtils.success(chatMessageService.listByConversationId(id));
    }

    @Operation(summary = "保存消息并自动生成标题")
    @PostMapping("/conversation/{id}/messages")
    public BaseResponse<?> saveMessages(@PathVariable Long id, @RequestBody List<ChatMessage> messages) {
        messages.forEach(msg -> msg.setConversationId(id));
        chatMessageService.saveMessages(messages);

        // 取第一条用户消息的前20字作为会话标题（条件更新，仅当标题仍为默认值时生效）
        messages.stream()
                .filter(m -> "user".equals(m.getRole()))
                .findFirst()
                .ifPresent(userMsg -> {
                    String title = userMsg.getContent().length() > 20
                            ? userMsg.getContent().substring(0, 20) + "..."
                            : userMsg.getContent();
                    log.info("尝试更新会话标题: id={}, title={}", id, title);
                    conversationService.updateTitleIfDefault(id, title);
                });
        return ResultUtils.success(null);
    }
}
