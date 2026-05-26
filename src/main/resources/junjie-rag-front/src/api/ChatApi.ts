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

const token = localStorage.getItem("token");

// 发送消息接口
export const sendChatMessageApi = async (message: string): Promise<Response> => {

  return fetch(`${service.defaults.baseURL}${ChatApi.Chat}?message=${encodeURIComponent(message)}`,
  { headers: { Authorization: "" + token } });
};

// export const sendChatMessageApi = async (message: string): Promise<Response> => {
//   return fetch(`${service.defaults.baseURL}${ChatApi.Chat}?message=${encodeURIComponent(message)}`, {
//     headers: getAuthHeaders()
//   });
// };

// 发送RAG消息接口
export const sendRagChatMessageApi = async (message: string): Promise<Response> => {
  return fetch(`${service.defaults.baseURL}${ChatApi.RagChat}?message=${encodeURIComponent(message)}`,
  { headers: { Authorization: "" + token } });
};