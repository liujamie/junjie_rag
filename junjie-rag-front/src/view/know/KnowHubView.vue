<template>
  <div class="kb-page">
    <!-- Header Banner -->
    <div class="kb-header">
      <div class="kb-header-content">
        <div class="kb-header-info">
          <h1 class="kb-title">知识库</h1>
          <p class="kb-desc">上传文档，构建你的专属 RAG 知识库</p>
        </div>
        <div class="kb-stats">
          <div class="stat-item">
            <span class="stat-value">{{ storeFileTotal }}</span>
            <span class="stat-label">文档总数</span>
          </div>
          <div class="stat-divider" />
          <div class="stat-item">
            <span class="stat-value">{{ uploadingCount }}</span>
            <span class="stat-label">待上传</span>
          </div>
        </div>
      </div>
    </div>

    <div class="kb-body">
      <!-- Upload Zone -->
      <div
        class="upload-zone"
        :class="{ 'is-dragover': isDragOver, 'is-uploading': isUploading }"
        @dragover.prevent="isDragOver = true"
        @dragleave.prevent="isDragOver = false"
        @drop.prevent="handleDrop"
        @click="!isUploading && triggerFileInput()"
      >
        <input
          ref="fileInputRef"
          type="file"
          multiple
          hidden
          @change="handleFileInputChange"
        />
        <div class="upload-zone-content">
          <div class="upload-icon-wrap">
            <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
              <rect width="40" height="40" rx="12" fill="#0891B2" fill-opacity="0.1"/>
              <path d="M20 12V28M12 20H28" stroke="#0891B2" stroke-width="2.5" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="upload-zone-text">
            <template v-if="isProcessing">
              <span class="upload-primary">后台处理中，请稍候...</span>
              <span class="upload-secondary">文件正在解析和向量化</span>
            </template>
            <template v-else-if="isUploading">
              <span class="upload-primary">上传中 {{ uploadProgress }}%</span>
              <span class="upload-secondary">{{ fileList.length }} 个文件正在上传</span>
            </template>
            <template v-else>
              <span class="upload-primary">点击或拖拽文件上传</span>
              <span class="upload-secondary">支持 PDF、DOC、MD、Excel、TXT 等格式，最大 100MB</span>
            </template>
          </div>
        </div>
        <!-- Progress bar -->
        <div v-if="isUploading && uploadProgress > 0" class="upload-progress">
          <div class="progress-bar" :style="{ width: uploadProgress + '%' }"></div>
        </div>
        <div v-if="isProcessing" class="upload-progress">
          <div class="progress-bar is-processing"></div>
        </div>
        <!-- Floating file previews -->
        <div v-if="fileList?.length && !isUploading && !isProcessing" class="upload-pending">
          <el-tag
            v-for="(f, i) in fileList"
            :key="i"
            closable
            @close="removePendingFile(i)"
            class="pending-tag"
          >
            {{ f.name }}
          </el-tag>
        </div>
      </div>

      <!-- Toolbar -->
      <div class="kb-toolbar">
        <div class="toolbar-left">
          <el-button
            type="danger"
            plain
            :disabled="!hasSelection"
            @click="batchDelete"
            class="toolbar-btn"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
            批量删除
          </el-button>
          <el-button
            plain
            :disabled="!hasSelection"
            @click="batchDownload"
            class="toolbar-btn"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3"/></svg>
            批量下载
          </el-button>
        </div>
        <div class="toolbar-right">
          <div class="search-wrapper">
            <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
            <input
              v-model="queryFileDto.fileName"
              placeholder="搜索文件..."
              class="search-input"
              @keyup.enter="loadStoreFileData"
            />
          </div>
          <el-button @click="loadStoreFileData" class="toolbar-btn" plain>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 4v6h6M23 20v-6h-6"/><path d="M20.49 9A9 9 0 005.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 013.51 15"/></svg>
          </el-button>
          <el-button
            v-if="fileList?.length"
            type="primary"
            :loading="isUploading"
            @click="uploadFile"
            class="upload-btn"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12"/></svg>
            上传 ({{ fileList.length }})
          </el-button>
        </div>
      </div>

      <!-- File Grid -->
      <div v-loading="isLoading" class="file-grid-wrap">
        <div v-if="storeFileData.length === 0 && !isLoading" class="empty-state">
          <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
            <rect width="64" height="64" rx="16" fill="#F1F5F9"/>
            <path d="M24 20h10l6 6v18a2 2 0 01-2 2H24a2 2 0 01-2-2V22a2 2 0 012-2z" stroke="#94A3B8" stroke-width="2"/>
            <path d="M34 20v6h6" stroke="#94A3B8" stroke-width="2"/>
          </svg>
          <p class="empty-title">还没有文档</p>
          <p class="empty-desc">拖拽或点击上方区域上传文件</p>
        </div>
        <div v-else class="file-grid">
          <div
            v-for="(file, index) in storeFileData"
            :key="file.id"
            class="file-card"
            :class="{ 'is-selected': selectedIds.has(file.id) }"
            @click="toggleSelect(file.id)"
          >
            <div class="file-card-check">
              <div class="check-box" :class="{ checked: selectedIds.has(file.id) }">
                <svg v-if="selectedIds.has(file.id)" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3"><path d="M5 12l5 5 9-9"/></svg>
              </div>
            </div>
            <div class="file-card-icon" :style="{ background: getFileColor(file.fileName) }">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.5">
                <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
                <path d="M14 2v6h6"/>
              </svg>
            </div>
            <div class="file-card-body">
              <p class="file-name" :title="file.fileName">{{ file.fileName }}</p>
              <p class="file-meta">{{ format(new Date(file.createTime), "yyyy-MM-dd HH:mm") }}</p>
            </div>
            <div class="file-card-actions" @click.stop>
              <button class="action-btn" title="下载" @click="openFilePreview(file)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3"/></svg>
              </button>
              <button class="action-btn danger" title="删除" @click="deleteStoreFile(file)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div class="kb-footer">
        <el-pagination
          v-model:current-page="queryFileDto.page"
          v-model:page-size="queryFileDto.pageSize"
          :page-sizes="[12, 24, 48, 96]"
          :total="storeFileTotal"
          layout="total, sizes, prev, pager, next"
          background
          small
          @size-change="loadStoreFileData"
          @current-change="loadStoreFileData"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import { uploadFileApi, queryFileApi, deleteFileApi, downloadFileApi } from "@/api/KnowHubApi"
import type { StoreFile } from "@/api/data"
import type { QueryFileDto } from "@/api/dto"
import { format } from "date-fns"

const storeFileData = ref<StoreFile[]>([])
const queryFileDto = ref<QueryFileDto>({ page: 1, pageSize: 12, fileName: "" })
const isUploading = ref(false)
const uploadProgress = ref(0)
const isProcessing = ref(false)
const isLoading = ref(false)
const storeFileTotal = ref(0)
const selectedIds = ref(new Set<number>())
const isDragOver = ref(false)
const fileList = ref<File[]>([])
const fileInputRef = ref<HTMLInputElement>()

const hasSelection = computed(() => selectedIds.value.size > 0)
const uploadingCount = computed(() => fileList.value.length)

const loadStoreFileData = () => {
  isLoading.value = true
  const params = { ...queryFileDto.value }
  queryFileApi(params).then((res) => {
    if (res.code == 0) {
      const page = res.data
      storeFileTotal.value = page.total ?? 0
      storeFileData.value = page.records ?? []
    } else { ElMessage({ type: "error", message: res.message }) }
  }).catch((err) => ElMessage({ type: "error", message: err }))
    .finally(() => { isLoading.value = false })
}

const triggerFileInput = () => fileInputRef.value?.click()

const handleFileInputChange = (e: Event) => {
  const input = e.target as HTMLInputElement
  if (input.files) fileList.value = [...fileList.value, ...Array.from(input.files)]
  input.value = ""
}

const handleDrop = (e: DragEvent) => {
  isDragOver.value = false
  if (e.dataTransfer?.files) fileList.value = [...fileList.value, ...Array.from(e.dataTransfer.files)]
}

const removePendingFile = (i: number) => {
  fileList.value.splice(i, 1)
}

const uploadFile = () => {
  if (!fileList.value.length) return
  const maxSize = 100 * 1024 * 1024
  for (const file of fileList.value) {
    if (file.size > maxSize) { ElMessage({ type: "error", message: `${file.name} 超过了最大上传大小 (100MB)` }); return }
  }
  isUploading.value = true
  uploadProgress.value = 0
  const uploadedNames = fileList.value.map(f => f.name)
  uploadFileApi(fileList.value, (p) => { uploadProgress.value = p }).then((res) => {
    if (res.code == 0) {
      isUploading.value = false
      isProcessing.value = true
      fileList.value = []
      uploadProgress.value = 0
      ElMessage({ type: "success", message: "上传完成，后台处理中..." })
      pollProcessingDone(uploadedNames)
    } else {
      isUploading.value = false
      uploadProgress.value = 0
      ElMessage({ type: "error", message: res.data.message })
    }
  }).catch((err) => {
    isUploading.value = false
    uploadProgress.value = 0
    ElMessage({ type: "error", message: err })
  })
}

let pollTimer: ReturnType<typeof setInterval> | null = null

const pollProcessingDone = (filenames: string[]) => {
  pollTimer = setInterval(() => {
    queryFileApi({ page: 1, pageSize: 999, fileName: "" }).then((res) => {
      if (res.code == 0) {
        const records: StoreFile[] = res.data.records ?? []
        const allDone = filenames.every(name =>
          records.some(r => r.fileName === name)
        )
        if (allDone) {
          if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
          isProcessing.value = false
          isUploading.value = false
          loadStoreFileData()
          ElMessage({ type: "success", message: "文件处理完成" })
        }
      }
    }).catch(() => {})
  }, 3000)
}

const deleteStoreFile = (file: StoreFile) => {
  ElMessageBox.confirm(`确定要删除"${file.fileName}"吗？`, "确认", { confirmButtonText: "确定", cancelButtonText: "取消", type: "warning" })
    .then(() => {
      deleteFileApi({ ids: file.id }).then((res) => {
        if (res.code == 0) { ElMessage({ type: "success", message: res.data }); selectedIds.value.delete(file.id); loadStoreFileData() }
      }).catch((err) => ElMessage({ type: "error", message: err }))
    }).catch(() => {})
}

const openFilePreview = async (file: StoreFile) => {
  try {
    const res = await downloadFileApi({ ids: file.id })
    if (res.code == 0 && res.data) {
      await downloadFile(res.data.url, res.data.name)
    } else {
      ElMessage({ type: "error", message: "获取下载链接失败" })
    }
  } catch (err) {
    ElMessage({ type: "error", message: String(err) })
  }
}

const toggleSelect = (id: number) => {
  const s = new Set(selectedIds.value)
  if (s.has(id)) s.delete(id); else s.add(id)
  selectedIds.value = s
}

const batchDelete = () => {
  if (!hasSelection.value) return
  ElMessageBox.confirm("确定要删除选中的文件吗？", "确认", { confirmButtonText: "确定", cancelButtonText: "取消", type: "warning" })
    .then(() => {
      const ids = [...selectedIds.value].join(",")
      deleteFileApi({ ids }).then((res) => {
        if (res.code == 0) { ElMessage({ type: "success", message: res.data }); selectedIds.value = new Set(); loadStoreFileData() }
      }).catch((err) => ElMessage({ type: "error", message: err }))
    }).catch(() => {})
}

const downloadFile = async (url: string, filename: string) => {
  const resp = await fetch(url)
  const blob = await resp.blob()
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = filename
  a.click()
  URL.revokeObjectURL(a.href)
}

const batchDownload = async () => {
  if (!hasSelection.value) return
  const ids = [...selectedIds.value].join(",")
  try {
    const res = await downloadFileApi({ ids })
    if (res.code == 0 && res.data) {
      const items: { url: string; name: string }[] = Array.isArray(res.data) ? res.data : [res.data]
      ElMessage({ type: 'info', message: `开始下载 ${items.length} 个文件...` })
      for (const item of items) {
        await downloadFile(item.url, item.name)
      }
      selectedIds.value = new Set()
      ElMessage({ type: 'success', message: '全部下载完成' })
    } else {
      ElMessage({ type: "error", message: "获取下载链接失败" })
    }
  } catch (err) {
    ElMessage({ type: "error", message: String(err) })
  }
}

const fileColors = ["#0891B2", "#6366F1", "#8B5CF6", "#EC4899", "#F59E0B", "#10B981"]
const getFileColor = (name: string) => fileColors[name.length % fileColors.length]

onMounted(() => { loadStoreFileData() })

onUnmounted(() => {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
})
</script>

<style scoped lang="less">
.kb-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #F8FAFC;
}

/* ===== Header ===== */
.kb-header {
  background: linear-gradient(135deg, #0F172A 0%, #1E293B 100%);
  padding: 28px 32px;
  flex-shrink: 0;
}

.kb-header-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kb-title {
  font-size: 22px;
  font-weight: 700;
  color: #FFFFFF;
  margin: 0 0 4px;
}

.kb-desc {
  font-size: 13px;
  color: #94A3B8;
  margin: 0;
}

.kb-stats {
  display: flex;
  align-items: center;
  gap: 24px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: #FFFFFF;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #64748B;
}

.stat-divider {
  width: 1px;
  height: 36px;
  background: #334155;
}

/* ===== Body ===== */
.kb-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}

/* ===== Upload Zone ===== */
.upload-zone {
  position: relative;
  border: 2px dashed #E2E8F0;
  border-radius: 16px;
  padding: 32px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #FFFFFF;
  margin-bottom: 20px;

  &:hover {
    border-color: #0891B2;
    background: #F0FDFA;
  }

  &.is-dragover {
    border-color: #0891B2;
    background: #ECFEFF;
    box-shadow: 0 0 0 4px rgba(8, 145, 178, 0.1);
  }

  &.is-uploading {
    cursor: not-allowed;
    pointer-events: auto;
  }
}

.upload-zone-content {
  display: flex;
  align-items: center;
  gap: 16px;
  justify-content: center;
}

.upload-icon-wrap {
  flex-shrink: 0;
}

.upload-zone-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.upload-primary {
  font-size: 14px;
  font-weight: 600;
  color: #1E293B;
}

.upload-secondary {
  font-size: 12px;
  color: #94A3B8;
}

/* Progress bar */
.upload-progress {
  margin-top: 16px;
  height: 6px;
  background: #E2E8F0;
  border-radius: 3px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #0891B2, #06B6D4);
  border-radius: 3px;
  transition: width 0.3s ease;

  &.is-processing {
    width: 100%;
    animation: progress-indeterminate 1.5s ease-in-out infinite;
    background: linear-gradient(90deg, #0891B2 0%, #06B6D4 40%, #0891B2 80%);
    background-size: 200% 100%;
  }
}

@keyframes progress-indeterminate {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.upload-pending {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 16px;
  justify-content: center;
}

.pending-tag {
  :deep(.el-tag__content) { font-size: 12px; }
}

/* ===== Toolbar ===== */
.kb-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-btn {
  font-size: 13px;
}

.search-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 12px;
  color: #94A3B8;
  pointer-events: none;
}

.search-input {
  width: 220px;
  height: 36px;
  padding: 0 12px 0 36px;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  font-size: 13px;
  color: #1E293B;
  background: #FFFFFF;
  outline: none;
  transition: all 0.2s ease;

  &:focus {
    border-color: #0891B2;
    box-shadow: 0 0 0 3px rgba(8, 145, 178, 0.1);
  }

  &::placeholder { color: #94A3B8; }
}

.upload-btn {
  font-size: 13px;
}

/* ===== File Grid ===== */
.file-grid-wrap {
  flex: 1;
  min-height: 200px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 8px;

  .empty-title { font-size: 16px; font-weight: 600; color: #64748B; margin: 0; }
  .empty-desc { font-size: 13px; color: #94A3B8; margin: 0; }
}

.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.file-card {
  position: relative;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;

  &:hover {
    border-color: #0891B2;
    box-shadow: 0 4px 12px rgba(8, 145, 178, 0.08);
    transform: translateY(-1px);
  }

  &.is-selected {
    border-color: #0891B2;
    background: #F0FDFA;
    box-shadow: 0 0 0 2px rgba(8, 145, 178, 0.15);
  }
}

.file-card-check {
  flex-shrink: 0;
}

.check-box {
  width: 20px;
  height: 20px;
  border: 2px solid #CBD5E1;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;

  &.checked {
    background: #0891B2;
    border-color: #0891B2;
  }
}

.file-card-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.file-card-body {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 13px;
  font-weight: 500;
  color: #1E293B;
  margin: 0 0 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  font-size: 11px;
  color: #94A3B8;
  margin: 0;
}

.file-card-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s ease;
  flex-shrink: 0;
}

.file-card:hover .file-card-actions {
  opacity: 1;
}

.action-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748B;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;

  &:hover {
    background: #F1F5F9;
    color: #0891B2;
  }

  &.danger:hover {
    background: #FEF2F2;
    color: #EF4444;
  }
}

/* ===== Footer ===== */
.kb-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  margin-top: 16px;
  border-top: 1px solid #E2E8F0;
  flex-shrink: 0;
}

/* ===== Scrollbar ===== */
.kb-body::-webkit-scrollbar { width: 6px; }
.kb-body::-webkit-scrollbar-thumb { background: #CBD5E1; border-radius: 3px; }
.kb-body::-webkit-scrollbar-track { background: transparent; }
</style>
