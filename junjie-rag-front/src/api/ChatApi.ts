import service from "@/http";
import { getAuthHeaders } from "@/http";

export const ChatApi = {
  Chat: "/chat/stream",
  RagChat: "/ai/rag",
};

// 聊天消息接口
export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  isTyping?: boolean;
}

// 发送消息接口
export const sendChatMessageApi = async (message: string, signal?: AbortSignal): Promise<Response> => {
  return fetch(`${service.defaults.baseURL}${ChatApi.Chat}?message=${encodeURIComponent(message)}`, {
    headers: getAuthHeaders(),
    signal
  });
};

// 发送RAG消息接口
export const sendRagChatMessageApi = async (message: string, signal?: AbortSignal): Promise<Response> => {
  return fetch(`${service.defaults.baseURL}${ChatApi.RagChat}?message=${encodeURIComponent(message)}`, {
    headers: getAuthHeaders(),
    signal
  });
};

// ===== 会话历史 API =====

// 获取会话列表
export const listConversationsApi = (page = 1, size = 50): Promise<any> => {
  return service.get(`/chat/conversations?page=${page}&size=${size}`);
};

// 新建会话
export const createConversationApi = (title?: string): Promise<any> => {
  return service.post('/chat/conversation', title ? { title } : {});
};

// 更新会话标题
export const updateConversationTitleApi = (id: number, title: string): Promise<any> => {
  return service.put(`/chat/conversation/${id}/title`, { title });
};

// 删除会话
export const deleteConversationApi = (id: number): Promise<any> => {
  return service.delete(`/chat/conversation/${id}`);
};

// 获取会话的消息列表
export const listMessagesApi = (conversationId: number): Promise<any> => {
  return service.get(`/chat/conversation/${conversationId}/messages`);
};

// 批量保存消息到会话
export const saveMessagesApi = (conversationId: number, messages: { role: string; content: string }[]): Promise<any> => {
  return service.post(`/chat/conversation/${conversationId}/messages`, messages);
};
