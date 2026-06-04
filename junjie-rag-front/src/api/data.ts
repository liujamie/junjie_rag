export interface StoreFile {
  id: number;
  url: string;
  fileName: string;
  vectorId: string[];
  createTime: Date;
  updateTime: Date;
}

export interface ConversationInfo {
  id: number;
  userId: number;
  title: string;
  status: number;
  createdTime: string;
  updatedTime: string;
}

export interface ChatMessageInfo {
  id: number;
  conversationId: number;
  role: string;
  content: string;
  createdTime: string;
}
