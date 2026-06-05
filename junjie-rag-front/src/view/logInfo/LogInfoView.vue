<template>
  <div class="page-container">
    <!-- Gradient Header -->
    <div class="page-header">
      <div class="page-header-content">
        <div>
          <h1 class="page-title">操作日志</h1>
          <p class="page-desc">查看系统的操作记录，支持按条件检索</p>
        </div>
      </div>
    </div>

    <!-- Content -->
    <div class="page-body">
      <!-- Search -->
      <div class="search-section">
        <el-form :inline="true" :model="searchForm" ref="searchFormRef">
          <el-form-item label="方法名" prop="methodName">
            <el-input v-model="searchForm.methodName" placeholder="方法名" clearable />
          </el-form-item>
          <el-form-item label="类名" prop="className">
            <el-input v-model="searchForm.className" placeholder="类名" clearable />
          </el-form-item>
          <el-form-item label="请求参数" prop="requestParams">
            <el-input v-model="searchForm.requestParams" placeholder="请求参数" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="toolbar">
        <el-button type="danger" @click="handleBatchDelete">清空日志</el-button>
      </div>

      <div v-if="logList.length === 0 && !isLoading" class="empty-state">
        <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
          <rect width="64" height="64" rx="16" fill="#F1F5F9"/>
          <rect x="16" y="16" width="32" height="4" rx="2" fill="#CBD5E1"/>
          <rect x="16" y="24" width="24" height="4" rx="2" fill="#E2E8F0"/>
          <rect x="16" y="32" width="28" height="4" rx="2" fill="#E2E8F0"/>
          <rect x="16" y="40" width="20" height="4" rx="2" fill="#E2E8F0"/>
          <circle cx="48" cy="48" r="8" fill="#E2E8F0"/>
          <path d="M46 48l2 2 4-4" stroke="#94A3B8" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
        <p class="empty-title">暂无日志记录</p>
        <p class="empty-desc">系统操作日志将显示在这里</p>
      </div>
      <el-table
        v-else
        v-loading="isLoading"
        :data="logList"
        class="data-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="methodName" label="方法名" width="150" show-overflow-tooltip />
        <el-table-column prop="className" label="类名" min-width="250" show-overflow-tooltip />
        <el-table-column prop="requestTime" label="产生时间" width="180">
          <template #default="scope">{{ new Date(scope.row.requestTime).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="requestParams" label="请求参数" min-width="200" show-overflow-tooltip />
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { queryLogApi, batchDeleteLogApi, type LogInfo, type LogQueryParams } from '@/api/LogApi'
import type { FormInstance } from 'element-plus'

const logList = ref<LogInfo[]>([])
const total = ref(0)
const isLoading = ref(false)
const selectedIds = ref<string[]>([])
const searchFormRef = ref<FormInstance>()
const queryParams = ref<LogQueryParams>({ page: 1, size: 10, methodName: '', className: '', requestParams: '' })
const searchForm = ref({ methodName: '', className: '', requestParams: '' })

const loadLogData = async () => {
  isLoading.value = true
  try {
    const params = { ...queryParams.value, page: queryParams.value.page - 1 }
    const res = await queryLogApi(params)
    if (res.code === 0) { logList.value = res.data.records; total.value = res.data.total }
    else { ElMessage.error(res.message || '获取日志列表失败') }
  } catch { ElMessage.error('获取日志列表失败') }
  finally { isLoading.value = false }
}

const handleSelectionChange = (selection: LogInfo[]) => { selectedIds.value = selection.map(item => item.id.toString()) }

const handleBatchDelete = () => {
  ElMessageBox.confirm('确定要清除所有日志吗？', '确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(async () => {
      try { const res = await batchDeleteLogApi(); if (res.code === 0) { ElMessage.success('删除成功'); loadLogData() } else { ElMessage.error(res.message || '删除失败') } }
      catch { ElMessage.error('删除失败') }
    }).catch(() => { ElMessage.info('已取消删除') })
}

const handleSearch = () => { queryParams.value = { ...queryParams.value, ...searchForm.value, page: 1 }; loadLogData() }

const resetSearch = () => {
  if (searchFormRef.value) searchFormRef.value.resetFields()
  queryParams.value = { page: 1, size: 10, methodName: '', className: '', requestParams: '' }
  loadLogData()
}

const handleSizeChange = (val: number) => { queryParams.value.size = val; queryParams.value.page = 1; loadLogData() }
const handleCurrentChange = (val: number) => { queryParams.value.page = val; loadLogData() }

onMounted(() => { loadLogData() })
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
  padding: 20px 32px;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
  overflow-y: auto;
}

.search-section {
  margin-bottom: 16px;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 10px;
  padding: 16px 20px;

  :deep(.el-form-item) {
    margin-bottom: 0;
    margin-right: 12px;
  }
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
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
