<template>
  <div class="login-page">
    <!-- Decorative background -->
    <div class="bg-decoration">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
    </div>

    <div class="login-container" v-if="!isLoggedIn">
      <div class="login-card">
        <div class="login-header">
          <div class="login-logo">
            <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
              <rect width="40" height="40" rx="12" fill="#0891B2"/>
              <path d="M12 20L18 26L28 14" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <h1 class="login-title">欢迎回来</h1>
          <p class="login-subtitle">登录到 LIU-RAG-AI 知识库系统</p>
        </div>

        <el-form
          :model="loginForm"
          ref="formRef"
          class="login-form"
          v-loading="isLoading"
          element-loading-text="处理中..."
        >
          <el-form-item prop="userName" :rules="[{ required: true, message: '请输入用户名', trigger: 'blur' }]">
            <el-input v-model="loginForm.userName" placeholder="用户名" :prefix-icon="User" size="large" />
          </el-form-item>
          <el-form-item prop="password" :rules="[{ required: true, message: '请输入密码', trigger: 'blur' }]">
            <el-input v-model="loginForm.password" type="password" placeholder="密码" :prefix-icon="Lock" size="large" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="login-btn" @click="handleLogin" :loading="isLoading">登 录</el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <span>还没有账号？</span>
          <el-button text type="primary" @click="showRegisterDialog">立即注册</el-button>
        </div>
      </div>
    </div>

    <!-- Logged in state -->
    <div v-else class="welcome-container">
      <div class="welcome-card">
        <div class="welcome-avatar">
          <el-avatar :size="72" style="background: linear-gradient(135deg, #0891B2, #22D3EE)">
            {{ userInfo.userName?.charAt(0)?.toUpperCase() }}
          </el-avatar>
        </div>
        <h1 class="welcome-title">欢迎回来，{{ userInfo.name }}</h1>
        <p class="welcome-desc">基于RAG技术的个人知识库AI问答系统</p>
        <el-button type="primary" size="large" class="start-chat-btn" @click="goToChat">
          开始对话
          <el-icon class="arrow-icon"><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div class="user-profile">
        <el-dropdown @command="handleCommand">
          <el-avatar :size="40" style="background: #0891B2; cursor: pointer">
            {{ userInfo.userName?.charAt(0)?.toUpperCase() }}
          </el-avatar>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- Dialogs -->
    <el-dialog v-model="profileDialogVisible" title="个人信息" width="500px" :close-on-click-modal="false" class="profile-dialog">
      <div class="user-info" v-if="!isEditing">
        <div class="info-header">
          <el-avatar :size="56" style="background: linear-gradient(135deg, #0891B2, #22D3EE); font-size: 24px;">
            {{ userInfo.userName?.charAt(0)?.toUpperCase() }}
          </el-avatar>
          <div>
            <h3>{{ userInfo.name }}</h3>
            <span class="info-username">@{{ userInfo.userName }}</span>
          </div>
        </div>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label"><el-icon><Iphone /></el-icon> 手机号</span>
            <span class="info-value">{{ userInfo.phone || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label"><el-icon><Male /></el-icon> 性别</span>
            <span class="info-value">{{ userInfo.sex || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label"><el-icon><Document /></el-icon> 身份证号</span>
            <span class="info-value">{{ userInfo.idNumber || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label"><el-icon><Timer /></el-icon> 创建时间</span>
            <span class="info-value">{{ userInfo.createTime || '-' }}</span>
          </div>
        </div>
        <div class="info-actions">
          <el-button @click="profileDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="startEdit">修改信息</el-button>
        </div>
      </div>

      <el-form v-else :model="editForm" ref="editFormRef" label-width="100px">
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="editForm.userName" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" />
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-select v-model="editForm.sex">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="身份证号" prop="idNumber">
          <el-input v-model="editForm.idNumber" />
        </el-form-item>
      </el-form>
      <template #footer v-if="isEditing">
        <el-button @click="cancelEdit">取消</el-button>
        <el-button type="primary" @click="handleUpdate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="registerDialogVisible" title="注册账号" width="420px" :close-on-click-modal="false" class="register-dialog">
      <el-form :model="registerForm" ref="registerFormRef" label-width="0">
        <el-form-item prop="userName" :rules="[{ required: true, message: '请输入用户名', trigger: 'blur' }]">
          <el-input v-model="registerForm.userName" placeholder="用户名" :prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password" :rules="[{ required: true, message: '请输入密码', trigger: 'blur' }]">
          <el-input v-model="registerForm.password" type="password" placeholder="密码" :prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item prop="phone" :rules="[{ required: true, message: '请输入手机号', trigger: 'blur' }]">
          <el-input v-model="registerForm.phone" placeholder="手机号" :prefix-icon="Iphone" size="large" />
        </el-form-item>
        <el-form-item prop="name" :rules="[{ required: true, message: '请输入姓名', trigger: 'blur' }]">
          <el-input v-model="registerForm.name" placeholder="姓名" :prefix-icon="UserFilled" size="large" />
        </el-form-item>
        <el-form-item prop="sex">
          <el-select v-model="registerForm.sex" placeholder="选择性别" size="large" style="width: 100%">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item prop="idNumber" :rules="[{ required: true, message: '请输入身份证号', trigger: 'blur' }]">
          <el-input v-model="registerForm.idNumber" placeholder="身份证号" size="large" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerDialogVisible = false" size="large">取消</el-button>
        <el-button type="primary" @click="handleRegister" size="large" :loading="isLoading">注册</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="460px" :close-on-click-modal="false" class="password-dialog">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="0">
        <el-form-item prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="原密码" :prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="新密码" :prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="确认新密码" :prefix-icon="Lock" size="large" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false" size="large">取消</el-button>
        <el-button type="primary" @click="handleUpdatePassword" size="large" :loading="isLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { User, Lock, Iphone, UserFilled, Male, Document, Timer, ArrowRight } from '@element-plus/icons-vue'
import router from '@/router'
import { BASE_URL } from '@/http/config'
import { updatePasswordApi } from '@/api/UserApi'

interface UserInfo {
  id: number; name: string; userName: string; password: string; phone: string;
  sex: string; idNumber: string; status: number;
  createTime: string; updateTime: string;
  createUser: string | null; updateUser: string | null;
}

const loginForm = ref({ userName: '', password: '' })
const registerForm = ref({ name: '', userName: '', password: '', phone: '', sex: '男', idNumber: '', status: 1 })
const registerDialogVisible = ref(false)
const isLoggedIn = ref(false)
const userInfo = ref<UserInfo>({ id: 0, name: '', userName: '', password: '', phone: '', sex: '', idNumber: '', status: 1, createTime: '', updateTime: '', createUser: null, updateUser: null })
const profileDialogVisible = ref(false)

const isLoading = ref(false)
const isEditing = ref(false)
const editForm = ref({ id: 0, userName: '', name: '', phone: '', sex: '', idNumber: '' })
const passwordDialogVisible = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = ref({ id: 0, oldPassword: '', newPassword: '', confirmPassword: '' })

const fetchUserInfo = async () => {
  try {
    const token = localStorage.getItem('token')
    const userId = localStorage.getItem('userId')
    if (!token || !userId) return
    const response = await fetch(BASE_URL + `/user/${userId}`, { headers: { 'Authorization': `Bearer ${token}` } })
    const data = await response.json()
    if (data.code === 0) { userInfo.value = data.data; isLoggedIn.value = true }
  } catch (error) { console.error('获取用户信息失败:', error) }
}

const goToChat = () => router.push('/ragChat')

const handleCommand = (command: string) => {
  if (command === 'profile') profileDialogVisible.value = true
  else if (command === 'password') showPasswordDialog()
  else if (command === 'logout') {
    localStorage.removeItem('token'); localStorage.removeItem('userRole'); localStorage.removeItem('userId')
    isLoggedIn.value = false
    router.push('/login')
    ElMessage({ message: '已成功退出登录', type: 'success' })
  }
}

const handleLogin = async () => {
  try {
    isLoading.value = true
    const response = await fetch(BASE_URL + `/user/login?userName=${loginForm.value.userName}&password=${loginForm.value.password}`, { method: 'POST' })
    const data = await response.json()
    if (data.code === 0) {
      localStorage.setItem('token', data.data.token); localStorage.setItem('userRole', data.data.role); localStorage.setItem('userId', data.data.id)
      ElMessage({ message: '登录成功', type: 'success' })
      await fetchUserInfo()
    } else {
      ElMessage({ message: data.message, type: 'error' })
    }
  } catch { ElMessage({ message: '登录失败，请稍后重试', type: 'error' }) }
  finally { isLoading.value = false }
}

const handleRegister = async () => {
  try {
    isLoading.value = true
    const response = await fetch(BASE_URL + '/user/register', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(registerForm.value) })
    const data = await response.json()
    if (data.code === 0) {
      ElMessage({ message: '注册成功', type: 'success' })
      registerDialogVisible.value = false
      registerForm.value = { name: '', userName: '', password: '', phone: '', sex: '男', idNumber: '', status: 1 }
    } else { ElMessage({ message: data.message, type: 'error' }) }
  } catch { ElMessage({ message: '注册失败，请稍后重试', type: 'error' }) }
  finally { isLoading.value = false }
}

const showRegisterDialog = () => { registerDialogVisible.value = true }

const startEdit = () => {
  editForm.value = { id: userInfo.value.id, userName: userInfo.value.userName, name: userInfo.value.name, phone: userInfo.value.phone, sex: userInfo.value.sex, idNumber: userInfo.value.idNumber }
  isEditing.value = true
}
const cancelEdit = () => { isEditing.value = false }

const handleUpdate = async () => {
  try {
    isLoading.value = true
    const token = localStorage.getItem('token')
    const response = await fetch(BASE_URL + '/user/update', { method: 'PUT', headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` }, body: JSON.stringify(editForm.value) })
    const data = await response.json()
    if (data.code === 0) { ElMessage({ message: '更新成功', type: 'success' }); await fetchUserInfo(); isEditing.value = false }
    else { ElMessage({ message: data.message || '更新失败', type: 'error' }) }
  } catch { ElMessage({ message: '更新失败，请稍后重试', type: 'error' }) }
  finally { isLoading.value = false }
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }, { min: 5, max: 20, message: '密码长度在 5 到 20 个字符', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 5, max: 20, message: '密码长度在 5 到 20 个字符', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }, { min: 5, max: 20, message: '密码长度在 5 到 20 个字符', trigger: 'blur' }, { validator: (_rule: any, value: string, callback: any) => { value !== passwordForm.value.newPassword ? callback(new Error('两次输入的密码不一致')) : callback() }, trigger: 'blur' }]
}

const showPasswordDialog = () => {
  passwordForm.value = { id: userInfo.value.id, oldPassword: '', newPassword: '', confirmPassword: '' }
  passwordDialogVisible.value = true
}

const handleUpdatePassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        isLoading.value = true
        const response = await updatePasswordApi(passwordForm.value)
        if (response.code === 0) { ElMessage.success('密码修改成功'); passwordDialogVisible.value = false }
        else { ElMessage.error(response.message || '密码修改失败') }
      } catch { ElMessage.error('密码修改失败，请稍后重试') }
      finally { isLoading.value = false }
    }
  })
}

onMounted(() => {
  const token = localStorage.getItem('token')
  if (token) fetchUserInfo()
})
</script>

<style scoped lang="less">
.login-page {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #F0F9FF 0%, #ECFEFF 50%, #F0FDF4 100%);
}

/* Background decoration */
.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
}

.bg-circle-1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(8, 145, 178, 0.08) 0%, transparent 70%);
  top: -100px;
  right: -100px;
}

.bg-circle-2 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.08) 0%, transparent 70%);
  bottom: -50px;
  left: -50px;
}

.bg-circle-3 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(34, 197, 94, 0.06) 0%, transparent 70%);
  top: 40%;
  left: 10%;
}

/* Login card */
.login-card {
  width: 400px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.7);
  position: relative;
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;

  .login-logo {
    margin-bottom: 16px;
  }

  .login-title {
    font-size: 24px;
    font-weight: 700;
    color: var(--text-primary);
    margin-bottom: 6px;
  }

  .login-subtitle {
    color: var(--text-muted);
    font-size: 14px;
  }
}

.login-form {
  .el-form-item {
    margin-bottom: 20px;
  }

  .login-btn {
    width: 100%;
    height: 46px;
    font-size: 15px;
    font-weight: 500;
    border-radius: 10px;
  }
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 13px;
  color: var(--text-secondary);

  .el-button {
    font-size: 13px;
  }
}

/* Welcome state */
.welcome-container {
  text-align: center;
  position: relative;
  z-index: 1;
}

.welcome-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 56px 48px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.7);
  max-width: 480px;
}

.welcome-avatar {
  margin-bottom: 20px;
}

.welcome-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.welcome-desc {
  color: var(--text-muted);
  margin-bottom: 32px;
}

.start-chat-btn {
  height: 46px;
  padding: 0 32px;
  font-size: 15px;
  border-radius: 10px;

  .arrow-icon {
    margin-left: 6px;
  }
}

.user-profile {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 100;
}

/* Profile dialog */
.user-info {
  .info-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 24px;
    padding-bottom: 20px;
    border-bottom: 1px solid var(--border-color);

    h3 { font-size: 18px; font-weight: 600; margin: 0; }
    .info-username { color: var(--text-muted); font-size: 13px; }
  }
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 24px;
}

.info-item {
  padding: 12px;
  border-radius: 8px;
  background: #F8FAFC;
  display: flex;
  flex-direction: column;
  gap: 4px;

  .info-label {
    font-size: 12px;
    color: var(--text-muted);
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .info-value {
    font-size: 14px;
    color: var(--text-primary);
    font-weight: 500;
  }
}

.info-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* Dialog overrides */
.profile-dialog, .register-dialog, .password-dialog {
  :deep(.el-dialog__body) {
    padding: 16px 24px;
  }
}

/* Responsive */
@media (max-width: 480px) {
  .login-card {
    width: calc(100% - 32px);
    padding: 32px 24px;
  }
}
</style>
