<template>
  <div class="settings-page p-6 max-w-screen-xl mx-auto">
    <!-- 顶部标题 -->
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-slate-800 mb-2">设置</h1>
      <p class="text-slate-500">管理您的账户和应用偏好</p>
    </div>

    <div class="flex flex-col md:flex-row gap-6">
      <!-- 左侧导航 -->
      <div class="w-full md:w-64 flex-shrink-0">
        <el-card shadow="never" class="rounded-xl border-none shadow-sm">
          <el-menu
            :default-active="activeTab"
            class="border-none w-full !border-r-0"
            @select="handleMenuSelect"
          >
            <el-menu-item index="profile">
              <el-icon><User /></el-icon>
              <span>个人信息</span>
            </el-menu-item>
            <el-menu-item index="security">
              <el-icon><Lock /></el-icon>
              <span>安全设置</span>
            </el-menu-item>
            <el-menu-item index="preference">
              <el-icon><Setting /></el-icon>
              <span>偏好设置</span>
            </el-menu-item>
          </el-menu>
        </el-card>
      </div>

      <!-- 右侧内容 -->
      <div class="flex-1">
        <el-card shadow="never" class="rounded-xl border-none shadow-sm min-h-[600px]">
          <!-- 个人信息面板 -->
          <div v-if="activeTab === 'profile'" class="p-4">
            <h2 class="text-lg font-medium text-slate-800 mb-8">个人信息</h2>

            <el-form :model="formState" label-position="top" class="max-w-3xl">
              <div class="mb-8">
                <h3 class="text-sm font-medium text-slate-500 mb-4">基本信息</h3>
                <el-form-item label="用户名" prop="userName">
                  <el-input v-model="formState.userName" size="large" placeholder="请输入用户名" />
                </el-form-item>

                <el-form-item label="个人简介" prop="userProfile">
                   <el-input
                     v-model="formState.userProfile"
                     type="textarea"
                     placeholder="介绍一下自己..."
                     :rows="4"
                     size="large"
                   />
                </el-form-item>
              </div>

              <div class="mb-8">
                <h3 class="text-sm font-medium text-slate-500 mb-4">头像</h3>
                <div class="flex items-center gap-6">
                   <el-avatar :size="80" :src="formState.userAvatar" class="flex-shrink-0 text-2xl bg-blue-100 text-blue-600">
                      {{ formState.userName?.charAt(0)?.toUpperCase() }}
                   </el-avatar>
                   <div>
                       <el-upload
                         name="file"
                         :show-file-list="false"
                         :http-request="handleUpload"
                         :before-upload="beforeUpload"
                       >
                         <el-button type="primary" link class="font-medium cursor-pointer">上传新头像</el-button>
                       </el-upload>
                       <p class="text-slate-400 text-xs mt-1">支持 JPG / PNG / WebP 格式，10MB以内</p>
                   </div>
                </div>
              </div>

              <div class="flex justify-end gap-3 mt-12 pt-6 border-t border-slate-100">
                 <el-button size="large" @click="handleReset">取消</el-button>
                 <el-button type="primary" size="large" :loading="loading" @click="handleSave">保存更改</el-button>
              </div>
            </el-form>
          </div>

          <!-- 安全设置面板 -->
          <div v-if="activeTab === 'security'" class="p-4">
              <!-- Security List View -->
              <div v-if="securityView === 'list'">
                  <!-- Account Security -->
                  <div class="mb-10">
                      <h3 class="text-base font-bold text-slate-800 mb-4">账户安全</h3>
                      <p class="text-slate-500 text-sm mb-6">管理您的登录凭证和安全设置</p>

                      <div class="space-y-4">
                          <!-- Email -->
                          <div class="border border-slate-200 rounded-lg p-6 flex items-center justify-between hover:border-blue-300 transition-colors">
                              <div class="flex items-center gap-4">
                                  <div class="w-10 h-10 rounded-full bg-blue-50 text-blue-500 flex items-center justify-center text-xl">
                                      <el-icon><Message /></el-icon>
                                  </div>
                                  <div>
                                      <div class="font-medium text-slate-700 text-base">邮箱地址</div>
                                      <div class="text-slate-500 text-sm mt-1">{{ currentUser.email || '未绑定' }}</div>
                                  </div>
                              </div>
                              <el-button type="primary" link class="!text-slate-400 hover:!text-blue-600" @click="securityView = 'email'">修改 <el-icon class="ml-1"><ArrowRight /></el-icon></el-button>
                          </div>

                          <!-- Password -->
                          <div class="border border-slate-200 rounded-lg p-6 flex items-center justify-between hover:border-blue-300 transition-colors">
                              <div class="flex items-center gap-4">
                                  <div class="w-10 h-10 rounded-full bg-blue-50 text-blue-500 flex items-center justify-center text-xl">
                                      <el-icon><Lock /></el-icon>
                                  </div>
                                  <div>
                                      <div class="font-medium text-slate-700 text-base">登录密码</div>
                                      <div class="text-slate-500 text-sm mt-1">定期更换密码可以提高账户安全性</div>
                                  </div>
                              </div>
                              <el-button type="primary" link class="!text-slate-400 hover:!text-blue-600" @click="securityView = 'password'">修改 <el-icon class="ml-1"><ArrowRight /></el-icon></el-button>
                          </div>
                      </div>
                  </div>
              </div>

              <!-- Update Email View -->
              <div v-if="securityView === 'email'">
                  <div class="mb-8 cursor-pointer text-slate-500 hover:text-blue-600 flex items-center gap-2" @click="securityView = 'list'">
                       <el-icon><ArrowLeft /></el-icon> 返回设置
                  </div>
                  <h2 class="text-xl font-bold text-slate-800 mb-2">修改邮箱</h2>
                  <p class="text-slate-500 mb-8">更新您的邮箱地址</p>

                  <el-alert title="注意: 修改邮箱后，您可以使用新邮箱或用户名为登录名登录。" type="info" show-icon class="mb-8" :closable="false" />

                   <el-form label-position="top" class="max-w-lg">
                       <el-form-item label="当前邮箱">
                           <el-input size="large" :value="currentUser.email" disabled class="bg-gray-50" />
                       </el-form-item>

                       <el-form-item label="新邮箱" required>
                           <el-input v-model="emailForm.newEmail" size="large" placeholder="请输入新的邮箱地址" />
                       </el-form-item>

                       <el-form-item label="验证码" required>
                           <div class="flex gap-4 w-full">
                               <el-input v-model="emailForm.code" size="large" placeholder="请输入6位验证码" />
                               <el-button size="large" :loading="codeLoading" :disabled="countdown > 0" @click="handleSendCode">
                                   {{ countdown > 0 ? `${countdown}秒后重试` : '发送验证码' }}
                               </el-button>
                           </div>
                       </el-form-item>

                       <div class="flex justify-end gap-3 mt-8">
                           <el-button size="large" link @click="securityView = 'list'">重置</el-button>
                           <el-button type="primary" size="large" :loading="emailLoading" @click="handleUpdateEmail">确认修改</el-button>
                       </div>
                   </el-form>
              </div>

              <!-- Update Password View -->
              <div v-if="securityView === 'password'">
                   <div class="mb-8 cursor-pointer text-slate-500 hover:text-blue-600 flex items-center gap-2" @click="securityView = 'list'">
                       <el-icon><ArrowLeft /></el-icon> 返回设置
                  </div>
                  <h2 class="text-xl font-bold text-slate-800 mb-2">修改密码</h2>
                  <p class="text-slate-500 mb-8">更新您的登录密码</p>

                   <el-alert title="密码要求: 密码长度8-20位，必须包含字母、数字、特殊符号等三种字符" type="info" show-icon class="mb-8" :closable="false" />

                   <el-form label-position="top" class="max-w-lg">
                       <el-form-item label="当前密码" required>
                           <el-input v-model="passwordForm.oldPassword" type="password" show-password size="large" placeholder="请输入当前密码" />
                       </el-form-item>

                       <el-form-item label="新密码" required>
                           <el-input v-model="passwordForm.newPassword" type="password" show-password size="large" placeholder="请输入新密码" />
                       </el-form-item>

                       <el-form-item label="确认新密码" required>
                           <el-input v-model="passwordForm.checkPassword" type="password" show-password size="large" placeholder="请再次输入新密码" />
                       </el-form-item>

                       <div class="flex justify-end gap-3 mt-8">
                           <el-button size="large" link @click="securityView = 'list'">重置</el-button>
                           <el-button type="primary" size="large" :loading="passwordLoading" @click="handleUpdatePassword">更新密码</el-button>
                       </div>
                   </el-form>
              </div>
          </div>

          <!-- 偏好设置面板 -->
          <div v-if="activeTab === 'preference'" class="p-4 flex items-center justify-center h-[400px] text-slate-400">
             偏好设置功能开发中...
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import {
  ArrowLeft,
  ArrowRight,
  Lock,
  Message,
  Setting,
  User
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getLoginUser, updateMyInfo, updateUserEmail, updateUserPassword, sendCode } from '~/api/sysUserController'
import { uploadFile } from '~/api/fileController'

const activeTab = ref('profile')
// securityView: 'list' | 'email' | 'password'
const securityView = ref('list')
const loading = ref(false)
const currentUser = ref<API.SysUserVO>({})

const formState = reactive({
  id: 0 as any,
  userName: '',
  userAvatar: '',
  userProfile: '',
})

// Update Email Form
const emailForm = reactive({
    newEmail: '',
    code: ''
})
const emailLoading = ref(false)
const codeLoading = ref(false)
const countdown = ref(0)
let timer: any = null

// Update Password Form
const passwordForm = reactive({
    oldPassword: '',
    newPassword: '',
    checkPassword: ''
})
const passwordLoading = ref(false)

const handleMenuSelect = (key: string) => {
    activeTab.value = key
}

// Watch tab change
watch(activeTab, (val: string) => {
    if (val === 'security') {
         securityView.value = 'list' // reset to list view
    }
})

const fetchUserInfo = async () => {
  try {
    const res = await getLoginUser() as any
    // request.ts returns response.data directly (interceptor)
    // so res is { code: 0, data: UserVO, message: "ok" }
    if (res.code === 0 && res.data) {
      currentUser.value = res.data
      // 初始化表单
      formState.id = currentUser.value.id!
      formState.userName = currentUser.value.userName || ''
      formState.userAvatar = currentUser.value.userAvatar || ''
      formState.userProfile = currentUser.value.userProfile || ''
    }
  } catch (error) {
    console.error(error)
  }
}

const handleReset = () => {
    formState.userName = currentUser.value?.userName || ''
    formState.userAvatar = currentUser.value?.userAvatar || ''
    formState.userProfile = currentUser.value?.userProfile || ''
    ElMessage.info('已重置更改')
}


const handleSave = async () => {
  if (!formState.userName) {
    ElMessage.error('用户名不能为空')
    return
  }

  loading.value = true
  try {
    const res = await updateMyInfo({
      id: formState.id,
      userName: formState.userName,
      userAvatar: formState.userAvatar,
      userProfile: formState.userProfile
    }) as any

    if (res.code === 0) {
      ElMessage.success('保存成功')
      await fetchUserInfo()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (error: unknown) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// Logic for sending email code
const handleSendCode = async () => {
  if (!emailForm.newEmail) {
    ElMessage.error('请输入新邮箱地址')
    return
  }
  // Simple regex
  if (!/^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/.test(emailForm.newEmail)) {
    ElMessage.error('邮箱格式不正确')
    return
  }

  codeLoading.value = true
  try {
    const res = await sendCode({ email: emailForm.newEmail }) as any
    if (res.code === 0) {
      ElMessage.success('验证码发送成功')
      // Start countdown
      countdown.value = 60
      timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    } else {
      ElMessage.error(res.message || '发送失败')
    }
  } catch (e) {
    // ElMessage.error('发送验证码失败')
  } finally {
    codeLoading.value = false
  }
}

const handleUpdateEmail = async () => {
  if (!emailForm.newEmail || !emailForm.code) {
    ElMessage.error('请填写完整信息')
    return
  }
  emailLoading.value = true
  try {
    const res = await updateUserEmail({
      newEmail: emailForm.newEmail,
      code: emailForm.code
    }) as any
    if (res.code === 0) {
      ElMessage.success('邮箱修改成功')
      securityView.value = 'list' // go back
      // Reset form
      emailForm.newEmail = ''
      emailForm.code = ''
      if (timer) clearInterval(timer)
      countdown.value = 0
      fetchUserInfo() // refresh user info
    } else {
      ElMessage.error(res.message || '修改失败')
    }
  } catch (e) {
    // ElMessage.error('修改失败')
  } finally {
    emailLoading.value = false
  }
}

const handleUpdatePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.checkPassword) {
    ElMessage.error('请填写完整信息')
    return
  }
  if (passwordForm.newPassword !== passwordForm.checkPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  // simple length check
  if (passwordForm.newPassword.length < 8) {
    ElMessage.warning('建议密码长度不少于8位')
  }

  passwordLoading.value = true
  try {
    const res = await updateUserPassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      checkPassword: passwordForm.checkPassword
    }) as any
    if (res.code === 0) {
      ElMessage.success('密码修改成功，请重新登录')
      // Redirect or logout logic
      window.location.reload()
    } else {
      ElMessage.error(res.message || '修改失败')
    }
  } catch (e) {
    // ElMessage.error('修改失败')
  } finally {
    passwordLoading.value = false
  }
}


// 图像上传逻辑
const beforeUpload = (file: File) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/webp';
  if (!isJpgOrPng) {
    ElMessage.error('只支持 JPG/PNG/WEBP 格式图片!');
  }
  const isLt2M = file.size / 1024 / 1024 < 10;
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 10MB!');
  }
  return isJpgOrPng && isLt2M;
}

const handleUpload = async (options: any) => {
  const { file, onSuccess, onError } = options
  const formData = new FormData();
  formData.append('file', file);

  try {
    const res = await uploadFile(formData) as any;
    if (res.code === 0) {
      formState.userAvatar = res.data || '';
      // Also update backend user record immediately or wait for save?
      // Typically avatar upload is independent or just gets the URL.
      // Here we just set the URL and user needs to click "Save Changes".
      ElMessage.success('头像上传成功');
      onSuccess(res.data);
    } else {
      ElMessage.error('上传失败: ' + (res.message || 'Unknown error'));
      onError();
    }
  } catch (error: unknown) {
    console.error(error)
    ElMessage.error('上传出错');
    onError();
  }
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style scoped>
:deep(.el-menu-item.is-active) {
    background-color: #eff6ff !important;
    color: #2563eb !important;
    font-weight: 500;
}
:deep(.el-menu-item) {
    height: 48px;
    line-height: 48px;
    margin-bottom: 8px;
    border-radius: 8px;
    color: #64748b;
}
:deep(.el-menu-item:hover) {
    color: #334155;
    background-color: #f8fafc;
}
</style>
