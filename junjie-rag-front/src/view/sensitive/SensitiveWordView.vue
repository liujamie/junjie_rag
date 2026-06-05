<template>
  <div class="page-container">
    <!-- Gradient Header -->
    <div class="page-header">
      <div class="page-header-content">
        <div>
          <h1 class="page-title">敏感词管理</h1>
          <p class="page-desc">管理系统中的敏感词汇，支持批量添加和删除</p>
        </div>
        <div class="page-header-actions">
          <el-button type="primary" @click="handleAdd">新增敏感词</el-button>
          <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">批量删除</el-button>
        </div>
      </div>
    </div>

    <!-- Content -->
    <div class="page-body">
      <div v-if="sensitiveList.length === 0 && !isLoading" class="empty-state">
        <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
          <rect width="64" height="64" rx="16" fill="#F1F5F9"/>
          <circle cx="32" cy="24" r="8" stroke="#94A3B8" stroke-width="2"/>
          <path d="M20 44c0-6.627 5.373-12 12-12s12 5.373 12 12" stroke="#94A3B8" stroke-width="2"/>
          <line x1="28" y1="18" x2="36" y2="18" stroke="#CBD5E1" stroke-width="2"/>
        </svg>
        <p class="empty-title">还没有敏感词</p>
        <p class="empty-desc">点击上方"新增敏感词"添加</p>
      </div>
      <el-table
        v-else
        v-loading="isLoading"
        :data="sensitiveList"
        class="data-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="word" label="敏感词" min-width="180" />
        <el-table-column prop="category" label="类别" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.category === '1' ? 'warning' : 'info'" effect="plain" round>{{ scope.row.category === '1' ? '违禁词' : '其他' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === '1' ? 'success' : 'danger'" effect="plain" round>
              {{ scope.row.status === '1' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <el-button type="danger" size="small" text @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="新增敏感词" width="480px">
      <el-form ref="formRef" :model="sensitiveForm" :rules="formRules" label-width="100px">
        <el-form-item label="敏感词" prop="word">
          <el-input v-model="sensitiveForm.word" placeholder="请输入敏感词" />
        </el-form-item>
        <el-form-item label="类别" prop="category">
          <el-select v-model="sensitiveForm.category" placeholder="请选择类别" style="width: 100%">
            <el-option label="违禁词" value="1" />
            <el-option label="其他" value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { querySensitiveApi, addSensitiveApi, batchDeleteSensitiveApi, type SensitiveInfo } from '@/api/SensitiveApi'

const sensitiveList = ref<SensitiveInfo[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const isLoading = ref(false)
const queryParams = ref({ page: 1, size: 10 })
const sensitiveForm = ref({ word: '', category: '1' })
const formRules: FormRules = { word: [{ required: true, message: '请输入敏感词', trigger: 'blur' }], category: [{ required: true, message: '请选择类别', trigger: 'change' }] }

const loadSensitiveList = async () => {
  isLoading.value = true
  try {
    const params = { page: queryParams.value.page - 1, size: queryParams.value.size }
    const res = await querySensitiveApi(params)
    if (res.code === 0) { sensitiveList.value = res.data.records; total.value = res.data.total }
    else { ElMessage.error(res.message || '获取敏感词列表失败') }
  } catch { ElMessage.error('获取敏感词列表失败') }
  finally { isLoading.value = false }
}

const handleSelectionChange = (selection: SensitiveInfo[]) => { selectedIds.value = selection.map(item => item.id) }
const handleAdd = () => { sensitiveForm.value = { word: '', category: '1' }; dialogVisible.value = true }

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const res = await addSensitiveApi(sensitiveForm.value)
        if (res.code === 0) { ElMessage.success('添加成功'); dialogVisible.value = false; loadSensitiveList() }
        else { ElMessage.error(res.message || '添加失败') }
      } catch { ElMessage.error('添加失败') }
    }
  })
}

const handleDelete = (row: SensitiveInfo) => { handleBatchDelete([row.id]) }

const handleBatchDelete = (ids?: number[]) => {
  const deleteIds = ids || selectedIds.value
  if (deleteIds.length === 0) return
  ElMessageBox.confirm(`确定要删除选中的 ${deleteIds.length} 条敏感词吗？`, '确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(async () => {
      try { const res = await batchDeleteSensitiveApi(deleteIds); if (res.code === 0) { ElMessage.success('删除成功'); loadSensitiveList() } else { ElMessage.error(res.message || '删除失败') } }
      catch { ElMessage.error('删除失败') }
    }).catch(() => { ElMessage.info('已取消删除') })
}

const handleSizeChange = (val: number) => { queryParams.value.size = val; queryParams.value.page = 1; loadSensitiveList() }
const handleCurrentChange = (val: number) => { queryParams.value.page = val; loadSensitiveList() }

onMounted(() => { loadSensitiveList() })
</script>

<style scoped lang="less">
.page-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #F8FAFC;
}

/* ===== Gradient Header ===== */
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

.page-header-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

/* ===== Content ===== */
.page-body {
  flex: 1;
  padding: 20px 32px;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
  overflow-y: auto;
}

.data-table {
  width: 100%;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 8px;
  background: #FFFFFF;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);

  .empty-title { font-size: 15px; font-weight: 600; color: #64748B; margin: 0; }
  .empty-desc { font-size: 13px; color: #94A3B8; margin: 0; }
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
