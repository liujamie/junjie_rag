<template>
  <div class="model-settings">
    <div class="page-header">
      <h2>🎛️ 模型管理</h2>
      <p class="page-desc">切换和管理 LLM 模型，支持自定义接入</p>
    </div>

    <div class="page-body">
      <div class="settings-layout">
        <!-- 左侧模型列表 -->
        <div class="model-list-panel">
          <div class="panel-title">可用模型</div>
          <div class="model-list-scroll">
            <div
              v-for="m in models"
              :key="m.id"
              :class="['model-item', { active: activeId === m.id, 'is-current': currentId === m.id }]"
              @click="selectModel(m)"
            >
              <div class="model-item-top">
                <span class="model-provider-tag" :class="m.provider">{{ providerLabel(m.provider) }}</span>
                <el-tag v-if="m.isDefault" size="small" type="info" effect="plain">预置</el-tag>
                <el-tag v-if="currentId === m.id" size="small" type="success" effect="plain">当前</el-tag>
              </div>
              <div class="model-name">{{ m.name }}</div>
              <div class="model-sub">{{ m.modelName }}</div>
            </div>
          </div>
          <el-button class="add-btn" :icon="Plus" @click="showAddDialog = true">
            添加自定义模型
          </el-button>
        </div>

        <!-- 右侧配置面板 -->
        <div class="model-config-panel" v-if="selected">
          <div class="config-section">
            <div class="section-title">基本信息</div>
            <el-form label-width="100px" size="small">
              <el-form-item label="模型名称">
                <el-input v-model="selected.name" :disabled="selected.isDefault" />
              </el-form-item>
              <el-form-item label="提供商">
                <el-select v-model="selected.provider" :disabled="selected.isDefault" style="width:100%">
                  <el-option label="DeepSeek" value="deepseek" />
                  <el-option label="OpenAI" value="openai" />
                  <el-option label="Ollama(本地)" value="ollama" />
                </el-select>
              </el-form-item>
              <el-form-item label="接口地址">
                <el-input v-model="selected.baseUrl" :disabled="selected.isDefault" placeholder="https://api.deepseek.com" />
              </el-form-item>
              <el-form-item label="模型名">
                <el-input v-model="selected.modelName" :disabled="selected.isDefault" placeholder="deepseek-v4-flash" />
              </el-form-item>
              <el-form-item label="API Key">
                <el-input v-model="selected.apiKey" :disabled="selected.isDefault" show-password placeholder="可选" />
              </el-form-item>
            </el-form>
          </div>

          <div class="config-section">
            <div class="section-title">生成参数</div>
            <el-form label-width="100px" size="small">
              <el-form-item label="温度">
                <el-slider v-model="selected.temperature" :min="0" :max="2" :step="0.1" style="width:200px" />
                <span class="param-value">{{ selected.temperature }}</span>
              </el-form-item>
              <el-form-item label="最大Token">
                <el-input-number v-model="selected.maxTokens" :min="256" :max="32768" :step="256" />
              </el-form-item>
              <el-form-item label="Top P">
                <el-slider v-model="selected.topP" :min="0" :max="1" :step="0.05" style="width:200px" />
                <span class="param-value">{{ selected.topP }}</span>
              </el-form-item>
            </el-form>
          </div>

          <div class="config-actions">
            <el-button type="primary" size="default" @click="handleSwitch(selected)">✅ 切换到此模型</el-button>
            <el-button v-if="!selected.isDefault" size="default" @click="handleUpdate(selected)">💾 保存</el-button>
            <el-button v-if="!selected.isDefault" type="danger" size="default" @click="handleDelete(selected)">🗑️ 删除</el-button>
          </div>
        </div>
        <div class="model-config-panel empty-panel" v-else>
          <div class="empty-hint">请从左侧选择一个模型</div>
        </div>
      </div>
    </div>

    <!-- 添加自定义模型对话框 -->
    <el-dialog v-model="showAddDialog" title="添加自定义模型" width="520px" top="5vh" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" label-width="100px" size="small">
        <el-form-item label="模型名称" required>
          <el-input v-model="addForm.name" placeholder="我的本地模型" />
        </el-form-item>
        <el-form-item label="提供商" required>
          <el-select v-model="addForm.provider" style="width:100%">
            <el-option label="DeepSeek" value="deepseek" />
            <el-option label="OpenAI" value="openai" />
            <el-option label="Ollama(本地)" value="ollama" />
          </el-select>
        </el-form-item>
        <el-form-item label="接口地址" required>
          <el-input v-model="addForm.baseUrl" placeholder="http://localhost:11434" />
        </el-form-item>
        <el-form-item label="模型名" required>
          <el-input v-model="addForm.modelName" placeholder="qwen3:4b" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="addForm.apiKey" show-password placeholder="可选" />
        </el-form-item>
        <el-divider>生成参数（可选）</el-divider>
        <el-form-item label="温度">
          <el-slider v-model="addForm.temperature" :min="0" :max="2" :step="0.1" style="width:200px" />
          <span class="param-value">{{ addForm.temperature }}</span>
        </el-form-item>
        <el-form-item label="最大Token">
          <el-input-number v-model="addForm.maxTokens" :min="256" :max="32768" :step="256" />
        </el-form-item>
        <el-form-item label="Top P">
          <el-slider v-model="addForm.topP" :min="0" :max="1" :step="0.05" style="width:200px" />
          <span class="param-value">{{ addForm.topP }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  listModelsApi, switchModelApi, addModelApi,
  updateModelApi, deleteModelApi, getCurrentModelApi, type LlmModelConfig
} from '@/api/ModelApi'

const models = ref<LlmModelConfig[]>([])
const activeId = ref<number | null>(null)
const currentId = ref<number | null>(null)
const selected = ref<LlmModelConfig | null>(null)
const showAddDialog = ref(false)

const addForm = ref<LlmModelConfig>({
  name: '',
  provider: 'openai',
  baseUrl: '',
  modelName: '',
  apiKey: '',
  temperature: 0.7,
  maxTokens: 4096,
  topP: 0.9
})

const providerLabel = (p: string) => ({ deepseek: 'DeepSeek', openai: 'OpenAI', ollama: 'Ollama' }[p] || p)

const fetchModels = async () => {
  const res = await listModelsApi()
  if (res.code === 0) models.value = res.data || []
}

const fetchCurrentModel = async () => {
  const res = await getCurrentModelApi()
  if (res.code === 0) currentId.value = res.data
}

const selectModel = (m: LlmModelConfig) => {
  activeId.value = m.id!
  selected.value = { ...m }
}

const handleSwitch = async (m: LlmModelConfig) => {
  const res = await switchModelApi(m.id!)
  if (res.code === 0) {
    ElMessage.success(`已切换到: ${m.name}`)
    currentId.value = m.id!
  } else {
    ElMessage.error(res.message || '切换失败')
  }
}

const handleAdd = async () => {
  if (!addForm.value.name || !addForm.value.baseUrl || !addForm.value.modelName) {
    ElMessage.warning('请填写必填项')
    return
  }
  const res = await addModelApi(addForm.value)
  if (res.code === 0) {
    ElMessage.success('添加成功')
    showAddDialog.value = false
    addForm.value = { name: '', provider: 'openai', baseUrl: '', modelName: '', apiKey: '', temperature: 0.7, maxTokens: 4096, topP: 0.9 }
    await fetchModels()
  } else {
    ElMessage.error(res.message || '添加失败')
  }
}

const handleUpdate = async (m: LlmModelConfig) => {
  const res = await updateModelApi(m)
  if (res.code === 0) {
    ElMessage.success('保存成功')
    await fetchModels()
  } else {
    ElMessage.error(res.message || '保存失败')
  }
}

const handleDelete = async (m: LlmModelConfig) => {
  try {
    await ElMessageBox.confirm(`确定删除模型"${m.name}"？`, '确认', { type: 'warning' })
    const res = await deleteModelApi(m.id!)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      selected.value = null
      activeId.value = null
      await fetchModels()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch { /* cancelled */ }
}

onMounted(() => {
  fetchModels()
  fetchCurrentModel()
})
</script>

<style scoped lang="less">
.model-settings {
  height: 100%; display: flex; flex-direction: column;
  .page-header {
    background: linear-gradient(135deg, #0F172A, #1E293B);
    padding: 24px 32px; flex-shrink: 0;
    h2 { color: #fff; margin: 0 0 4px; font-size: 20px; }
    .page-desc { color: #94A3B8; margin: 0; font-size: 13px; }
  }
  .page-body {
    padding: 20px 32px; flex: 1; min-height: 0;
    overflow: hidden;
  }
}

.settings-layout {
  display: flex; gap: 20px; height: 100%;
  overflow: hidden;

  .model-list-panel {
    width: 260px; flex-shrink: 0; overflow-y: auto;
    background: #fff; border-radius: 10px;
    box-shadow: 0 1px 3px rgba(0,0,0,.06); padding: 16px;
    display: flex; flex-direction: column;

    .panel-title {
      font-size: 14px; font-weight: 600; margin-bottom: 12px; color: #0F172A;
      flex-shrink: 0;
    }
    .model-list-scroll { flex: 1; overflow-y: auto; min-height: 0; }

    .model-item {
      padding: 10px 12px; border-radius: 8px; cursor: pointer;
      margin-bottom: 6px; transition: all .15s;
      border: 1px solid transparent;
      &:hover { background: #F8FAFC; border-color: #E2E8F0; }
      &.active { background: #ECFEFF; border-color: #0891B2; }
      &.is-current { border-color: #10B981; background: #F0FDF4; }

      .model-item-top { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
      .model-provider-tag {
        font-size: 11px; padding: 0 6px; border-radius: 4px; line-height: 18px;
        &.deepseek { background: #FEF3C7; color: #92400E; }
        &.openai { background: #DBEAFE; color: #1E40AF; }
        &.ollama { background: #D1FAE5; color: #065F46; }
      }
      .model-name { font-size: 13px; font-weight: 500; color: #0F172A; }
      .model-sub { font-size: 12px; color: #94A3B8; margin-top: 2px; }
    }
    .add-btn { width: 100%; margin-top: 8px; flex-shrink: 0; }
  }

  .model-config-panel {
    flex: 1; overflow-y: auto; min-width: 0;
    background: #fff; border-radius: 10px;
    box-shadow: 0 1px 3px rgba(0,0,0,.06); padding: 20px 24px;
  }
  .empty-panel {
    display: flex; align-items: center; justify-content: center;
    .empty-hint { color: #94A3B8; font-size: 14px; }
  }
}

.config-section {
  margin-bottom: 20px;
  .section-title {
    font-size: 14px; font-weight: 600; color: #0F172A; margin-bottom: 12px;
    padding-bottom: 8px; border-bottom: 1px solid #E2E8F0;
  }
}

.param-value {
  margin-left: 10px; font-size: 13px; color: #0891B2; font-weight: 500; min-width: 30px;
}

.config-actions { margin-top: 24px; display: flex; gap: 10px; }
</style>
