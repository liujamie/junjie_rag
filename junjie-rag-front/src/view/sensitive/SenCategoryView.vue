<template>
  <div class="page-container">
    <el-card class="page-card">
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">新增分类</el-button>
        <el-button type="danger" :disabled="!selectedRows.length" @click="handleBatchDelete">批量删除</el-button>
      </div>

      <el-table
        v-loading="isLoading"
        :data="categoryList"
        class="data-table"
        border
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="categoryName" label="分类名称" min-width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === '1' ? 'success' : 'danger'" effect="plain">
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
    </el-card>

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
import { addCategoryApi, queryCategoryPageApi, updateCategoryApi, batchDeleteCategoryApi, type CategoryInfo, type CategoryUpdateDto } from '@/api/SensitiveApi'

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
