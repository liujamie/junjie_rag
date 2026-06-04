<template>
  <div class="page-container">
    <el-card class="page-card">
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

      <el-table
        v-loading="isLoading"
        :data="logList"
        class="data-table"
        border
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
    </el-card>
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
  height: calc(100vh - 32px);
  padding: 0;
  box-sizing: border-box;
}

.page-card {
  height: 100%;
  display: flex;
  flex-direction: column;

  :deep(.el-card__body) {
    flex: 1;
    display: flex;
    flex-direction: column;
    padding: 24px;
    overflow: hidden;
  }
}

.search-section {
  margin-bottom: 16px;

  .el-form {
    display: flex;
    flex-wrap: wrap;
    gap: 0;
  }

  :deep(.el-form-item) {
    margin-bottom: 0;
    margin-right: 12px;
  }
}

.toolbar {
  margin-bottom: 16px;
}

.data-table {
  flex: 1;
  min-height: 0;
}
</style>
