<template>
  <div class="page-container">
    <el-card class="page-card">
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">新增敏感词</el-button>
        <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <el-table
        v-loading="isLoading"
        :data="sensitiveList"
        class="data-table"
        border
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="word" label="敏感词" min-width="180" />
        <el-table-column prop="category" label="类别" width="120">
          <template #default="scope">
            <el-tag effect="plain">{{ scope.row.category === '1' ? '违禁词' : '其他' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === '1' ? 'success' : 'danger'" effect="plain">
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
    </el-card>

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

.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}

.data-table {
  flex: 1;
  min-height: 0;
}
</style>
