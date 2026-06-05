<template>
  <div class="page-container">
    <!-- Gradient Header -->
    <div class="page-header">
      <div class="page-header-content">
        <div>
          <h1 class="page-title">敏感词分类</h1>
          <p class="page-desc">管理敏感词分类，便于组织和批量操作</p>
        </div>
        <div class="page-header-actions">
          <el-button type="primary" @click="handleAdd">新增分类</el-button>
          <el-button type="danger" :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
        </div>
      </div>
    </div>

    <!-- Content -->
    <div class="page-body">
      <div v-if="categoryList.length === 0 && !isLoading" class="empty-state">
        <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
          <rect width="64" height="64" rx="16" fill="#F1F5F9"/>
          <rect x="16" y="14" width="32" height="10" rx="3" stroke="#94A3B8" stroke-width="2"/>
          <rect x="16" y="28" width="32" height="10" rx="3" stroke="#94A3B8" stroke-width="2" stroke-dasharray="2 2"/>
          <rect x="16" y="42" width="32" height="10" rx="3" stroke="#94A3B8" stroke-width="2" stroke-dasharray="2 2"/>
        </svg>
        <p class="empty-title">还没有分类</p>
        <p class="empty-desc">点击上方"新增分类"添加</p>
      </div>
      <el-table
        v-else
        v-loading="isLoading"
        :data="categoryList"
        class="data-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="categoryName" label="分类名称" min-width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === '1' ? 'success' : 'danger'" effect="plain" round>
              {{ scope.row.status === '1' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="180" />
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" text @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" text @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 30, 40]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" label-width="100px">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="categoryForm.categoryName" placeholder="请输入分类名称" />
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
import { addCategoryApi, queryCategoryPageApi, updateCategoryApi, batchDeleteCategoryApi, type CategoryInfo } from '@/api/SensitiveApi'

const categoryList = ref<CategoryInfo[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const isLoading = ref(false)
const selectedRows = ref<CategoryInfo[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const categoryFormRef = ref<FormInstance>()
const isEdit = ref(false)
const currentId = ref<number | null>(null)
const categoryForm = ref({ categoryName: '' })
const categoryRules: FormRules = { categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }, { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }] }

const loadCategoryData = async () => {
  isLoading.value = true
  try {
    const response = await queryCategoryPageApi({ page: currentPage.value, size: pageSize.value })
    if (response.code === 0) { categoryList.value = response.data.records; total.value = response.data.total }
    else { ElMessage.error(response.message || '获取数据失败') }
  } catch { ElMessage.error('获取数据失败') }
  finally { isLoading.value = false }
}

const handleCurrentChange = (val: number) => { currentPage.value = val; loadCategoryData() }
const handleSizeChange = (val: number) => { pageSize.value = val; currentPage.value = 1; loadCategoryData() }
const handleSelectionChange = (rows: CategoryInfo[]) => { selectedRows.value = rows }

const handleAdd = () => { isEdit.value = false; dialogTitle.value = '新增分类'; currentId.value = null; categoryForm.value = { categoryName: '' }; dialogVisible.value = true }

const handleEdit = (row: CategoryInfo) => { isEdit.value = true; dialogTitle.value = '编辑分类'; currentId.value = row.id; categoryForm.value = { categoryName: row.categoryName }; dialogVisible.value = true }

const handleDelete = (row: CategoryInfo) => {
  ElMessageBox.confirm(`确定要删除分类 ${row.categoryName} 吗？`, '确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(async () => {
      try { const response = await batchDeleteCategoryApi([row.id]); if (response.code === 0) { ElMessage.success('删除成功'); loadCategoryData() } else { ElMessage.error(response.message || '删除失败') } }
      catch { ElMessage.error('删除失败') }
    }).catch(() => { ElMessage.info('已取消删除') })
}

const handleBatchDelete = () => {
  if (!selectedRows.value.length) { ElMessage.warning('请选择要删除的分类'); return }
  ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 个分类吗？`, '确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(async () => {
      try { const ids = selectedRows.value.map(row => row.id); const response = await batchDeleteCategoryApi(ids); if (response.code === 0) { ElMessage.success('批量删除成功'); loadCategoryData() } else { ElMessage.error(response.message || '批量删除失败') } }
      catch { ElMessage.error('批量删除失败') }
    }).catch(() => { ElMessage.info('已取消批量删除') })
}

const submitForm = async () => {
  if (!categoryFormRef.value) return
  await categoryFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        let response
        if (isEdit.value && currentId.value) { response = await updateCategoryApi({ id: currentId.value, categoryName: categoryForm.value.categoryName }) }
        else { response = await addCategoryApi(categoryForm.value) }
        if (response.code === 0) { ElMessage.success(isEdit.value ? '更新成功' : '添加成功'); dialogVisible.value = false; loadCategoryData() }
        else { ElMessage.error(response.message || '操作失败') }
      } catch { ElMessage.error('操作失败') }
    }
  })
}

onMounted(() => { loadCategoryData() })
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

.page-header-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
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
