<template>
  <div class="chat-view">
    <!-- Sidebar -->
    <div class="chat-sidebar">
      <div class="chat-sidebar-header">
        <el-button type="primary" :icon="Plus" class="new-chat-btn" @click="handleNewConversation">
          新对话
        </el-button>
      </div>
      <div class="chat-history" v-loading="loadingConversations">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          :class="['history-item', { active: conv.id === currentConversationId }]"
          @click="switchConversation(conv)"
        >
          <el-icon size="14"><ChatDotRound /></el-icon>
          <span class="history-title">{{ conv.title }}</span>
          <el-button
            text
            size="small"
            class="delete-conv-btn"
            @click.stop="handleDeleteConversation(conv)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <div v-if="conversations.length === 0 && !loadingConversations" class="history-empty">
          暂无会话记录
        </div>
      </div>
    </div>

    <!-- Main chat area -->
    <div class="chat-main">
      <div class="messages-container" ref="messageContainer">
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-logo">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
              <rect width="48" height="48" rx="16" fill="#0891B2" fill-opacity="0.1"/>
              <path d="M16 24L22 30L32 18" stroke="#0891B2" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <h2 class="empty-title">有什么可以帮助你的？</h2>
          <p class="empty-desc">选择模式开始对话 — 普通回答 或 RAG知识库回答</p>
        </div>

        <div v-for="(message, index) in messages" :key="index"
             :class="['message-row', message.role === 'user' ? 'user-row' : 'assistant-row']">
          <div class="message-avatar" v-if="message.role === 'assistant'">
            <el-avatar :size="32" style="background: #0891B2">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/>
              </svg>
            </el-avatar>
          </div>
          <div class="message-bubble" :class="{ 'typing-bubble': message.isTyping }">
            <div class="message-text" v-html="renderMarkdown(message.content)"></div>
            <button
              v-if="!message.isTyping"
              class="copy-btn"
              title="复制"
              @click="copyMessage(message.content)"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
                <path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/>
              </svg>
            </button>
          </div>
          <div class="message-avatar" v-if="message.role === 'user'">
            <el-avatar :size="32" style="background: #6366F1">U</el-avatar>
          </div>
        </div>
      </div>

      <div class="input-area">
        <div class="input-wrapper">
          <el-input
            v-model="userInput"
            type="textarea"
            :rows="1"
            :autosize="{ minRows: 1, maxRows: 5 }"
            placeholder="输入你的问题..."
            @keydown.enter.prevent="handleSend"
            class="chat-input"
          />
          <div class="input-actions">
            <div class="mode-buttons">
              <el-button
                :type="currentMode === 'rag' ? 'primary' : 'default'"
                size="small"
                :icon="Connection"
                @click="currentMode = 'rag'"
                round
              >RAG</el-button>
              <el-button
                :type="currentMode === 'normal' ? 'primary' : 'default'"
                size="small"
                :icon="ChatDotRound"
                @click="currentMode = 'normal'"
                round
              >普通</el-button>
            </div>
            <el-button
              v-if="!isLoading"
              type="primary"
              :icon="Promotion"
              :disabled="!userInput.trim()"
              class="send-btn"
              circle
              @click="handleSend"
            />
            <el-button
              v-else
              type="danger"
              icon="Close"
              class="send-btn stop-btn"
              circle
              @click="handleStop"
            />
          </div>
        </div>
        <p class="input-footer">AI 生成的内容仅供参考，请验证重要信息。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { marked } from 'marked'
import { Plus, ChatDotRound, Connection, Promotion, Delete, Close } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  sendChatMessageApi,
  sendRagChatMessageApi,
  listConversationsApi,
  createConversationApi,
  deleteConversationApi,
  updateConversationTitleApi,
  listMessagesApi,
  saveMessagesApi,
  type ChatMessage
} from '@/api/ChatApi'
import type { ConversationInfo } from '@/api/data'

// --- State ---
const messages = ref<ChatMessage[]>([])
const userInput = ref('')
const isLoading = ref(false)
const abortController = ref<AbortController | null>(null)
const messageContainer = ref<HTMLElement | null>(null)
const currentMode = ref<'normal' | 'rag'>('rag')

// Conversation state
const conversations = ref<ConversationInfo[]>([])
const currentConversationId = ref<number | null>(null)
const loadingConversations = ref(false)
const hasSavedCurrentExchange = ref(false) // 避免重复保存

// --- Conversation API ---

const fetchConversations = async () => {
  loadingConversations.value = true
  try {
    const res = await listConversationsApi()
    if (res.code === 0) {
      conversations.value = res.data.records || []
    }
  } catch (e) {
    console.error('获取会话列表失败', e)
  } finally {
    loadingConversations.value = false
  }
}

const handleNewConversation = async () => {
  try {
    const res = await createConversationApi()
    if (res.code === 0) {
      const conv = res.data as ConversationInfo
      conversations.value.unshift(conv)
      await switchConversation(conv)
    }
  } catch (e) {
    console.error('创建会话失败', e)
  }
}

const switchConversation = async (conv: ConversationInfo) => {
  currentConversationId.value = conv.id
  messages.value = []
  try {
    const res = await listMessagesApi(conv.id)
    if (res.code === 0) {
      const list = (res.data || []) as any[]
      messages.value = list.map((m: any) => ({
        role: m.role as 'user' | 'assistant',
        content: m.content
      }))
      scrollToBottom()
    }
  } catch (e) {
    console.error('加载消息失败', e)
  }
}

const handleDeleteConversation = async (conv: ConversationInfo) => {
  try {
    await ElMessageBox.confirm(`确定要删除"${conv.title}"吗？`, '确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    const res = await deleteConversationApi(conv.id)
    if (res.code === 0) {
      conversations.value = conversations.value.filter(c => c.id !== conv.id)
      if (currentConversationId.value === conv.id) {
        currentConversationId.value = null
        messages.value = []
      }
      ElMessage.success('已删除')
    }
  } catch { /* cancelled */ }
}

// --- Save messages after stream ---

const saveExchange = async (userMsg: string, assistantMsg: string, isFirst: boolean) => {
  if (!currentConversationId.value) return
  try {
    const msgList = [
      { role: 'user', content: userMsg },
      { role: 'assistant', content: assistantMsg }
    ]
    await saveMessagesApi(currentConversationId.value, msgList)

    // 首条消息自动设置标题
    if (isFirst) {
      const title = userMsg.length > 20 ? userMsg.slice(0, 20) + '...' : userMsg
      await updateConversationTitleApi(currentConversationId.value, title)
      const conv = conversations.value.find(c => c.id === currentConversationId.value)
      if (conv) conv.title = title
    }
  } catch (e) {
    console.error('保存消息失败', e)
  }
}

// --- Send message ---

const handleSend = async () => {
  if (!userInput.value.trim() || isLoading.value) return

  // 确保有活跃会话
  if (!currentConversationId.value) {
    const res = await createConversationApi()
    if (res.code !== 0) return
    const conv = res.data as ConversationInfo
    conversations.value.unshift(conv)
    currentConversationId.value = conv.id
  }

  // 创建中止控制器，允许用户手动停止 AI 回复
  const controller = new AbortController()
  abortController.value = controller

  const api = (msg: string) => {
    const fn = currentMode.value === 'rag' ? sendRagChatMessageApi : sendChatMessageApi
    return fn(msg, controller.signal)
  }
  await sendMessage(api)
}

const handleStop = () => {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
}

const sendMessage = async (apiMethod: (message: string) => Promise<Response>) => {
  if (!userInput.value.trim() || isLoading.value) return

  const userContent = userInput.value
  const isFirst = messages.value.length === 0

  // 用户消息
  messages.value.push({ role: 'user', content: userContent })
  userInput.value = ''
  isLoading.value = true
  hasSavedCurrentExchange.value = false

  // assistant 占位 — 通过数组索引更新以触发 Vue 响应式
  const assistantIdx = messages.value.length
  messages.value.push({ role: 'assistant' as const, content: '', isTyping: true })
  let replyContent = ''

  try {
    const response = await apiMethod(userContent)
    if (!response.ok) throw new Error('网络请求失败')

    const reader = response.body?.getReader()
    if (!reader) throw new Error('无法读取响应数据')

    const decoder = new TextDecoder()

    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      const text = decoder.decode(value)
      // SSE: 多个事件以 \n\n 分隔，一个事件内多行 data: 表示原始内容含 \n
      const rawEvents = text.split('\n\n')
      for (const rawEvent of rawEvents) {
        if (rawEvent.trim() === '' || rawEvent.startsWith(':')) continue
        const dataParts: string[] = []
        for (const line of rawEvent.split('\n')) {
          if (line.startsWith('data:')) {
            dataParts.push(line.slice(5))
          }
        }
        // 同一事件内多段 data: 用 \n 拼接（保留原始换行）
        replyContent += dataParts.join('\n')
      }
      // 流式更新视图
      messages.value[assistantIdx].content = replyContent
      await nextTick()
      scrollToBottom()
    }

    if (!replyContent) {
      replyContent = '抱歉，没有获取到有效回复。'
      messages.value[assistantIdx].content = replyContent
    }
  } catch (error: any) {
    if (error?.name === 'AbortError') {
      // 用户手动中止，不显示错误
      console.log('对话已中止')
    } else {
      console.error('对话错误:', error)
      replyContent = '抱歉，发生了错误，请稍后重试。'
      messages.value[assistantIdx].content = replyContent
    }
  } finally {
    isLoading.value = false
    abortController.value = null
    messages.value[assistantIdx].isTyping = false
    scrollToBottom()

    // 流结束后保存消息到会话（中止时也保存已收到的内容）
    if (currentConversationId.value && !hasSavedCurrentExchange.value && replyContent) {
      hasSavedCurrentExchange.value = true
      await saveExchange(userContent, replyContent, isFirst)
    }
  }
}

// --- Utils ---

const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })
}

const copyMessage = async (content: string) => {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage({ message: '复制成功', type: 'success', duration: 2000 })
  } catch {
    ElMessage({ message: '复制失败', type: 'error', duration: 2000 })
  }
}

const renderMarkdown = (content: string) => {
  if (!content) return ''
  try {
    return marked(content, { breaks: false, gfm: true })
  } catch {
    return content
  }
}

onMounted(() => {
  fetchConversations()
})
</script>

<style scoped lang="less">
.chat-view {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* Sidebar */
.chat-sidebar {
  width: 240px;
  background: #F8FAFC;
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.chat-sidebar-header {
  padding: 16px;
}

.new-chat-btn {
  width: 100%;
  height: 40px;
  font-size: 13px;
  border-radius: 8px;
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  margin-bottom: 2px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 13px;
  transition: all 150ms ease;

  &:hover {
    background: #E2E8F0;
    color: var(--text-primary);

    .delete-conv-btn {
      opacity: 1;
    }
  }

  &.active {
    background: #E0F2FE;
    color: var(--primary);
    font-weight: 500;
  }
}

.history-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delete-conv-btn {
  opacity: 0;
  transition: opacity 150ms ease;
  flex-shrink: 0;
  color: var(--text-muted);

  &:hover {
    color: var(--danger);
  }
}

.history-empty {
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
  padding: 40px 16px;
}

/* Main chat area */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px 0;
}

/* Empty state */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 40px;

  .empty-logo { margin-bottom: 20px; }
  .empty-title { font-size: 22px; font-weight: 600; color: var(--text-primary); margin-bottom: 8px; }
  .empty-desc { font-size: 14px; color: var(--text-muted); }
}

/* Message rows */
.message-row {
  display: flex;
  gap: 12px;
  padding: 16px 48px;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;

  &.user-row {
    justify-content: flex-end;

    .message-bubble {
      background: #0891B2;
      color: white;
      border-radius: 18px 18px 4px 18px;
    }
  }

  &.assistant-row {
    .message-bubble {
      background: #F1F5F9;
      color: var(--text-primary);
      border-radius: 18px 18px 18px 4px;
    }
  }
}

.message-avatar {
  flex-shrink: 0;
  margin-top: 4px;
}

.message-bubble {
  max-width: 75%;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.6;
  position: relative;

  &:hover .copy-btn {
    opacity: 1;
  }
}

.copy-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: white;
  color: var(--text-muted);
  cursor: pointer;
  opacity: 0;
  transition: opacity 150ms ease, color 150ms ease;
  z-index: 1;

  &:hover {
    color: var(--text-primary);
    border-color: var(--primary);
  }
}

.user-row .copy-btn {
  background: #0E7490;
  border-color: rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.7);

  &:hover {
    background: #155E75;
    color: white;
    border-color: rgba(255, 255, 255, 0.4);
  }
}

.message-text {
  white-space: pre-wrap;
  word-break: break-word;

  :deep(p) { margin: 0 0 4px; &:last-child { margin-bottom: 0; } }
  :deep(pre) {
    background: #1E293B;
    color: #E2E8F0;
    padding: 14px;
    border-radius: 8px;
    overflow-x: auto;
    font-size: 13px;
    margin: 8px 0;
    font-family: 'Fira Code', monospace;
  }
  :deep(code) {
    font-family: 'Fira Code', monospace;
    background: #E2E8F0;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 13px;
  }
  :deep(pre code) { background: transparent; padding: 0; }
  :deep(ul), :deep(ol) { padding-left: 20px; margin: 8px 0; }
  :deep(table) {
    border-collapse: collapse;
    margin: 8px 0;
    width: 100%;
    font-size: 13px;
    th, td {
      border: 1px solid #CBD5E1;
      padding: 6px 10px;
      text-align: left;
    }
    th { background: #F1F5F9; font-weight: 600; }
    tr:nth-child(even) td { background: #F8FAFC; }
  }
  :deep(blockquote) {
    margin: 8px 0;
    padding-left: 12px;
    border-left: 3px solid var(--primary);
    color: var(--text-secondary);
  }
  :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
    margin: 12px 0 6px;
    font-weight: 600;
  }
}

.typing-bubble::after {
  content: '...';
  animation: ellipsis 1.5s infinite;
}

@keyframes ellipsis {
  0% { opacity: 0.3; }
  50% { opacity: 1; }
  100% { opacity: 0.3; }
}

/* Input area */
.input-area {
  padding: 0 48px 24px;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
}

.input-wrapper {
  background: white;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 12px 16px 8px;
  box-shadow: var(--shadow-md);
  transition: box-shadow var(--transition-base);

  &:focus-within {
    box-shadow: 0 0 0 2px rgba(8, 145, 178, 0.15), var(--shadow-md);
    border-color: var(--primary);
  }
}

.chat-input {
  :deep(.el-textarea__inner) {
    border: none !important;
    box-shadow: none !important;
    padding: 0;
    font-size: 14px;
    resize: none;
    background: transparent;

    &:focus { box-shadow: none !important; }
  }
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #F1F5F9;
}

.mode-buttons {
  display: flex;
  gap: 6px;
}

.send-btn:not(.is-disabled) {
  background: var(--primary);
  border-color: var(--primary);
}

.stop-btn {
  background: #EF4444 !important;
  border-color: #EF4444 !important;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(239, 68, 68, 0); }
}

.input-footer {
  text-align: center;
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 8px;
}
</style>
