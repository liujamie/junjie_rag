<template>
  <div class="page-container">
    <!-- Gradient Header -->
    <div class="page-header">
      <div class="page-header-content">
        <div>
          <h1 class="page-title">用户管理</h1>
          <p class="page-desc">管理系统用户账号，支持新增和编辑用户信息</p>
        </div>
        <div class="page-header-actions">
          <el-button type="primary" @click="handleAdd">新增用户</el-button>
        </div>
      </div>
    </div>

    <!-- Content -->
    <div class="page-body">
      <div v-if="userList.length === 0 && !isLoading" class="empty-state">
        <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
          <rect width="64" height="64" rx="16" fill="#F1F5F9"/>
          <circle cx="32" cy="22" r="8" stroke="#94A3B8" stroke-width="2"/>
          <path d="M16 48c0-8.837 7.163-16 16-16s16 7.163 16 16" stroke="#94A3B8" stroke-width="2"/>
          <circle cx="44" cy="18" r="4" fill="#E2E8F0"/>
          <path d="M40 28c0-2.2 1.8-4 4-4s4 1.8 4 4" fill="#E2E8F0"/>
        </svg>
        <p class="empty-title">还没有用户</p>
        <p class="empty-desc">点击上方"新增用户"添加</p>
      </div>
      <el-table
        v-else
        v-loading="isLoading"
        :data="userList"
        class="data-table"
      >
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="userName" label="用户名" width="130" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="sex" label="性别" width="80" />
        <el-table-column prop="idNumber" label="身份证号" width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" effect="plain" round>
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="userFormRef" :model="userForm" :rules="isEdit ? editRules : userRules" label-width="100px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="userForm.name" />
        </el-form-item>
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="userForm.userName" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="userForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="userForm.sex">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="身份证号" prop="idNumber">
          <el-input v-model="userForm.idNumber" />
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
import { queryFileApi, registerUserApi, updateUserApi } from '@/api/UserApi'
import { QueryFileDto } from "@/api/dto.ts"
import type { FormInstance, FormRules } from 'element-plus'

interface UserInfo { id: number; name: string; userName: string; password: string; phone: string; sex: string; idNumber: string; status: number; createTime: string; updateTime: string; createUser: string | null; updateUser: string | null }

const queryFileDto = ref<QueryFileDto>({ page: 1, pageSize: 10, fileName: "" })
const userList = ref<UserInfo[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const isLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const userFormRef = ref<FormInstance>()
const userForm = ref({ name: '', userName: '', password: '', phone: '', sex: '男', idNumber: '' })
const isEdit = ref(false)
const currentUserId = ref<number | null>(null)

const userRules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  sex: [{ required: true, message: '请选择性别', trigger: 'change' }],
  idNumber: [{ required: true, message: '请输入身份证号', trigger: 'blur' }, { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号', trigger: 'blur' }]
}
const editRules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  sex: [{ required: true, message: '请选择性别', trigger: 'change' }],
  idNumber: [{ required: true, message: '请输入身份证号', trigger: 'blur' }, { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号', trigger: 'blur' }]
}

const loadStoreFileData = () => {
  isLoading.value = true;
  const params = { ...queryFileDto.value, page: queryFileDto.value.page - 1 }
  queryFileApi(params).then((res) => {
    if (res.code == 0) { const data = res.data; total.value = data.total; userList.value = data.records }
    else { ElMessage({ type: "error", message: res.message }) }
  }).catch((err) => { ElMessage({ type: "error", message: err }) })
    .finally(() => { isLoading.value = false })
}

const handleCurrentChange = (val: number) => { currentPage.value = val; loadStoreFileData() }
const handleSizeChange = (val: number) => { pageSize.value = val; currentPage.value = 1; loadStoreFileData() }

const handleAdd = () => { isEdit.value = false; dialogTitle.value = '新增用户'; currentUserId.value = null; userForm.value = { name: '', userName: '', password: '', phone: '', sex: '男', idNumber: '' }; dialogVisible.value = true }

const handleEdit = (row: UserInfo) => { isEdit.value = true; dialogTitle.value = '编辑用户'; currentUserId.value = row.id; userForm.value = { name: row.name, userName: row.userName, password: '', phone: row.phone, sex: row.sex, idNumber: row.idNumber }; dialogVisible.value = true }

const handleDelete = (row: UserInfo) => {
  ElMessageBox.confirm(`确定要删除用户 ${row.name} 吗？`, '确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(() => { ElMessage.info('删除用户功能待实现') })
    .catch(() => { ElMessage.info('已取消删除') })
}

const submitForm = async () => {
  if (!userFormRef.value) return
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        let response
        if (isEdit.value && currentUserId.value) { response = await updateUserApi({ id: currentUserId.value, name: userForm.value.name, userName: userForm.value.userName, phone: userForm.value.phone, sex: userForm.value.sex, idNumber: userForm.value.idNumber }) }
        else { response = await registerUserApi(userForm.value) }
        if (response.code === 0) { ElMessage.success(isEdit.value ? '编辑用户成功' : '添加用户成功'); dialogVisible.value = false; loadStoreFileData() }
        else { ElMessage.error(response.message || '操作失败') }
      } catch { ElMessage.error('操作失败') }
    }
  })
}

onMounted(() => { loadStoreFileData() })
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
