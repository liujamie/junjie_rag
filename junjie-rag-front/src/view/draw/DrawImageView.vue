<template>
  <div class="page-container">
    <!-- Gradient Header -->
    <div class="page-header">
      <div class="page-header-content">
        <div>
          <h1 class="page-title">AI 绘画</h1>
          <p class="page-desc">描述你想要的图片，AI 将为你生成</p>
        </div>
      </div>
    </div>

    <!-- Content -->
    <div class="page-body">
      <div class="draw-layout">
        <!-- Input panel -->
        <div class="input-panel">
          <div class="prompt-box">
            <el-input
              v-model="prompt"
              type="textarea"
              :rows="6"
              placeholder="描述你所想象的图片，比如：一只在星空下奔跑的银色机械狼，赛博朋克风格..."
              class="prompt-input"
            />
          </div>

          <div class="action-row">
            <el-button
              type="primary"
              size="large"
              :loading="isLoading"
              :disabled="!prompt.trim()"
              class="generate-btn"
              @click="generateImage"
            >
              <template #icon>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/>
                  <rect x="8" y="6" width="8" height="4" rx="1"/><path d="M12 10v6"/><path d="M10 13h4"/>
                </svg>
              </template>
              {{ isLoading ? '生成中...' : '生成图片' }}
            </el-button>
            <el-button v-if="generatedImage" size="large" class="download-btn" @click="downloadImage">
              <template #icon>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
                </svg>
              </template>
              下载图片
            </el-button>
          </div>
        </div>

        <!-- Preview panel -->
        <div class="preview-panel" :class="{ 'has-image': generatedImage }">
          <div v-if="!generatedImage" class="preview-empty">
            <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
              <rect x="8" y="8" width="48" height="48" rx="12" stroke="#CBD5E1" stroke-width="2" stroke-dasharray="4 4"/>
              <path d="M24 32l6 6 10-12 8 10v4a2 2 0 01-2 2H18a2 2 0 01-2-2v-4l6-8z" fill="#E2E8F0"/>
              <circle cx="26" cy="22" r="4" fill="#E2E8F0"/>
            </svg>
            <p class="preview-empty-text">输入描述并点击生成</p>
          </div>
          <div v-else class="preview-content">
            <img :src="generatedImage" alt="生成的图片" class="generated-img" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { BASE_URL } from "@/http/config.ts"

const prompt = ref('')
const generatedImage = ref('')
const isLoading = ref(false)

const generateImage = async () => {
  if (!prompt.value) return
  isLoading.value = true
  try {
    const response = await fetch(BASE_URL + `/draw/image?prompt=${encodeURIComponent(prompt.value)}`)
    if (response.ok) {
      const blob = await response.blob()
      generatedImage.value = URL.createObjectURL(blob)
    }
  } catch (error) {
    console.error('生成图片失败:', error)
  } finally {
    isLoading.value = false
  }
}

const downloadImage = () => {
  if (!generatedImage.value) return
  const link = document.createElement('a')
  link.href = generatedImage.value
  link.download = `generated-image-${Date.now()}.png`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
</script>

<style scoped lang="less">
.page-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #F8FAFC;
}

.page-header {
  background: linear-gradient(135deg, #0F172A 0%, #1E293B 100%);
  padding: 24px 32px;
  flex-shrink: 0;
}

.page-header-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #FFFFFF;
  margin: 0 0 4px;
}

.page-desc {
  font-size: 13px;
  color: #94A3B8;
  margin: 0;
}

.page-body {
  flex: 1;
  padding: 24px 32px;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}

.draw-layout {
  display: flex;
  gap: 24px;
  height: 100%;
  overflow: hidden;
}

.input-panel {
  width: 380px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.prompt-box {
  margin-bottom: 16px;
}

.prompt-input {
  :deep(.el-textarea__inner) {
    border-radius: 10px;
    font-size: 14px;
    line-height: 1.6;
    padding: 14px;
  }
}

.action-row {
  display: flex;
  gap: 10px;
}

.generate-btn {
  flex: 1;
  height: 46px;
  font-size: 14px;
  border-radius: 10px;
}

.download-btn {
  height: 46px;
  border-radius: 10px;
}

.preview-panel {
  flex: 1;
  min-width: 0;
  border-radius: 12px;
  background: #F8FAFC;
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: all var(--transition-base);

  &.has-image {
    background: #0F172A;
    border-color: #1E293B;
  }
}

.preview-empty {
  text-align: center;

  svg { margin-bottom: 16px; }
}

.preview-empty-text {
  font-size: 14px;
  color: var(--text-muted);
  margin: 0;
}

.preview-content {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.generated-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}
</style>
