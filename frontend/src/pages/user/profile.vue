<template>
  <div class="settings-page max-w-screen-xl mx-auto">
    <!-- 顶部标题 -->
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-[var(--el-text-color-primary)] mb-2">{{ t('user.profileInfo.editTitle') }}</h1>
      <p class="text-[var(--el-text-color-secondary)]">{{ t('user.profileInfo.editSubtitle') }}</p>
    </div>

    <div class="flex flex-col md:flex-row gap-6">
      <!-- 左侧导航 -->
      <div class="w-full md:w-64 flex-shrink-0">
        <el-card shadow="never" class="!rounded-xl !border-none shadow-sm">
          <el-menu
            :default-active="activeTab"
            class="!border-none w-full !border-r-0"
            @select="handleMenuSelect"
          >
            <el-menu-item index="profile">
              <el-icon><User /></el-icon>
              <span>{{ t('user.profileMenu.profile') }}</span>
            </el-menu-item>
            <el-menu-item index="security">
              <el-icon><Lock /></el-icon>
              <span>{{ t('user.profileMenu.security') }}</span>
            </el-menu-item>
            <el-menu-item index="preference">
              <el-icon><Setting /></el-icon>
              <span>{{ t('user.profileMenu.preference') }}</span>
            </el-menu-item>
          </el-menu>
        </el-card>
      </div>

      <!-- 右侧内容 -->
      <div class="flex-1">
        <el-card shadow="never" class="!rounded-xl !border-none shadow-sm min-h-[600px]">
          <!-- 个人信息面板 -->
          <div v-if="activeTab === 'profile'" class="p-4">
            <h2 class="text-lg font-medium text-[var(--el-text-color-primary)] mb-8">{{ t('user.profileInfo.basicTitle') }}</h2>

            <el-form :model="formState" label-position="top" class="max-w-3xl">
              <div class="mb-8">
                <h3 class="text-sm font-medium text-[var(--el-text-color-secondary)] mb-4">{{ t('user.profileInfo.basicSubtitle') }}</h3>
                <el-form-item :label="t('user.profileInfo.userNameLabel')" prop="userName">
                  <el-input v-model="formState.userName" size="large" :placeholder="t('user.profileInfo.userNamePlaceholder')" />
                </el-form-item>

                <el-form-item :label="t('user.profileInfo.bioLabel')" prop="userProfile">
                   <el-input
                     v-model="formState.userProfile"
                     type="textarea"
                     :placeholder="t('user.profileInfo.bioPlaceholder')"
                     :rows="4"
                     size="large"
                   />
                </el-form-item>
              </div>

              <div class="mb-8">
                <h3 class="text-sm font-medium text-[var(--el-text-color-secondary)] mb-4">{{ t('user.profileInfo.avatarLabel') }}</h3>
                <div class="flex items-center gap-6">
                   <el-avatar :size="80" :src="formState.userAvatar" class="flex-shrink-0 text-2xl bg-[var(--el-color-primary-light-9)] text-[var(--el-color-primary)]">
                      {{ formState.userName?.charAt(0)?.toUpperCase() }}
                   </el-avatar>
                   <div>
                       <el-upload
                         name="file"
                         :show-file-list="false"
                         :http-request="handleUpload"
                         :before-upload="beforeUpload"
                       >
                         <el-button type="primary" link class="font-medium cursor-pointer">{{ t('user.profileInfo.uploadText') }}</el-button>
                       </el-upload>
                       <p class="text-[var(--el-text-color-placeholder)] text-xs mt-1">{{ t('user.profileInfo.uploadHint') }}</p>
                   </div>
                </div>
              </div>

              <div class="flex justify-end gap-3 mt-12 pt-6 border-t border-[var(--el-border-color-lighter)]">
                 <el-button size="large" @click="handleReset">{{ t('user.profileInfo.cancel') }}</el-button>
                 <el-button type="primary" size="large" :loading="loading" @click="handleSave">{{ t('user.profileInfo.save') }}</el-button>
              </div>
            </el-form>
          </div>

          <!-- 安全设置面板 -->
          <div v-if="activeTab === 'security'" class="p-4">
              <!-- Security List View -->
              <div v-if="securityView === 'list'">
                  <!-- Account Security -->
                  <div class="mb-10">
                      <h3 class="text-base font-bold text-[var(--el-text-color-primary)] mb-4">{{ t('user.security.title') }}</h3>
                      <p class="text-[var(--el-text-color-secondary)] text-sm mb-6">{{ t('user.security.subtitle') }}</p>

                      <div class="space-y-4">
                          <!-- Email -->
                          <div class="border border-[var(--el-border-color-lighter)] rounded-lg p-6 flex items-center justify-between hover:border-[var(--el-color-primary-light-5)] transition-colors">
                              <div class="flex items-center gap-4">
                                  <div class="w-10 h-10 rounded-full bg-[var(--el-color-primary-light-9)] text-[var(--el-color-primary)] flex items-center justify-center text-xl">
                                      <el-icon><Message /></el-icon>
                                  </div>
                                  <div>
                                      <div class="font-medium text-[var(--el-text-color-regular)] text-base">{{ t('user.security.email.label') }}</div>
                                      <div class="text-[var(--el-text-color-secondary)] text-sm mt-1">{{ currentUser.email || t('user.security.email.notBound') }}</div>
                                  </div>
                              </div>
                              <el-button type="primary" link class="!text-[var(--el-text-color-secondary)] hover:!text-[var(--el-color-primary)]" @click="securityView = 'email'">{{ t('user.security.email.change') }} <el-icon class="ml-1"><ArrowRight /></el-icon></el-button>
                          </div>

                          <!-- Password -->
                          <div class="border border-[var(--el-border-color-lighter)] rounded-lg p-6 flex items-center justify-between hover:border-[var(--el-color-primary-light-5)] transition-colors">
                              <div class="flex items-center gap-4">
                                  <div class="w-10 h-10 rounded-full bg-[var(--el-color-primary-light-9)] text-[var(--el-color-primary)] flex items-center justify-center text-xl">
                                      <el-icon><Lock /></el-icon>
                                  </div>
                                  <div>
                                      <div class="font-medium text-[var(--el-text-color-regular)] text-base">{{ t('user.security.password.label') }}</div>
                                      <div class="text-[var(--el-text-color-secondary)] text-sm mt-1">{{ t('user.security.password.desc') }}</div>
                                  </div>
                              </div>
                              <el-button type="primary" link class="!text-[var(--el-text-color-secondary)] hover:!text-[var(--el-color-primary)]" @click="securityView = 'password'">{{ t('user.security.password.change') }} <el-icon class="ml-1"><ArrowRight /></el-icon></el-button>
                          </div>
                      </div>
                  </div>
              </div>

              <!-- Update Email View -->
              <div v-if="securityView === 'email'">
                  <div class="mb-8 cursor-pointer text-[var(--el-text-color-secondary)] hover:text-[var(--el-color-primary)] flex items-center gap-2" @click="securityView = 'list'">
                       <el-icon><ArrowLeft /></el-icon> {{ t('user.security.back') }}
                  </div>
                  <h2 class="text-xl font-bold text-[var(--el-text-color-primary)] mb-2">{{ t('user.security.email.title') }}</h2>
                  <p class="text-[var(--el-text-color-secondary)] mb-8">{{ t('user.security.email.subtitle') }}</p>

                  <el-alert :title="t('user.security.email.warning')" type="info" show-icon class="mb-8" :closable="false" />

                   <el-form label-position="top" class="max-w-lg">
                       <el-form-item :label="t('user.security.email.current')">
                           <el-input size="large" :value="currentUser.email" disabled class="bg-gray-50" />
                       </el-form-item>

                       <el-form-item :label="t('user.security.email.new')" required>
                           <el-input v-model="emailForm.newEmail" size="large" :placeholder="t('user.security.email.newPlaceholder')" />
                       </el-form-item>

                       <el-form-item :label="t('user.security.email.code')" required>
                           <div class="flex gap-4 w-full">
                               <el-input v-model="emailForm.code" size="large" :placeholder="t('user.security.email.codePlaceholder')" />
                               <el-button size="large" :loading="codeLoading" :disabled="countdown > 0" @click="handleSendCode">
                                   {{ countdown > 0 ? `${countdown}${t('user.security.email.retry')}` : t('user.security.email.sendCode') }}
                               </el-button>
                           </div>
                       </el-form-item>

                       <div class="flex justify-end gap-3 mt-8">
                           <el-button size="large" link @click="securityView = 'list'">{{ t('user.security.email.reset') }}</el-button>
                           <el-button type="primary" size="large" :loading="emailLoading" @click="handleUpdateEmail">{{ t('user.security.email.confirm') }}</el-button>
                       </div>
                   </el-form>
              </div>

              <!-- Update Password View -->
              <div v-if="securityView === 'password'">
                   <div class="mb-8 cursor-pointer text-[var(--el-text-color-secondary)] hover:text-[var(--el-color-primary)] flex items-center gap-2" @click="securityView = 'list'">
                       <el-icon><ArrowLeft /></el-icon> {{ t('user.security.back') }}
                  </div>
                  <h2 class="text-xl font-bold text-[var(--el-text-color-primary)] mb-2">{{ t('user.security.password.title') }}</h2>
                  <p class="text-[var(--el-text-color-secondary)] mb-8">{{ t('user.security.password.subtitle') }}</p>

                   <el-alert :title="t('user.security.password.warning')" type="info" show-icon class="mb-8" :closable="false" />

                   <el-form label-position="top" class="max-w-lg">
                       <el-form-item :label="t('user.security.password.current')" required>
                           <el-input v-model="passwordForm.oldPassword" type="password" show-password size="large" :placeholder="t('user.security.password.currentPlaceholder')" />
                       </el-form-item>

                       <el-form-item :label="t('user.security.password.new')" required>
                           <el-input v-model="passwordForm.newPassword" type="password" show-password size="large" :placeholder="t('user.security.password.newPlaceholder')" />
                       </el-form-item>

                       <el-form-item :label="t('user.security.password.confirm')" required>
                           <el-input v-model="passwordForm.checkPassword" type="password" show-password size="large" :placeholder="t('user.security.password.confirmPlaceholder')" />
                       </el-form-item>

                       <div class="flex justify-end gap-3 mt-8">
                           <el-button size="large" link @click="securityView = 'list'">{{ t('user.security.password.reset') }}</el-button>
                           <el-button type="primary" size="large" :loading="passwordLoading" @click="handleUpdatePassword">{{ t('user.security.password.update') }}</el-button>
                       </div>
                   </el-form>
              </div>
          </div>

          <!-- 偏好设置面板 -->
          <div v-if="activeTab === 'preference'" class="p-4 flex items-center justify-center h-[400px] text-[var(--el-text-color-placeholder)]">
             {{ t('user.preference.developing') }}
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
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
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
    ElMessage.info(t('user.profileInfo.resetMessage'))
}


const handleSave = async () => {
  if (!formState.userName) {
    ElMessage.error(t('user.profileInfo.userNameRequired'))
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
      ElMessage.success(t('user.profileInfo.saveSuccess'))
      await fetchUserInfo()
    } else {
      ElMessage.error(res.message || t('user.profileInfo.saveFailed'))
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
    ElMessage.error(t('user.security.email.inputNew'))
    return
  }
  // Simple regex
  if (!/^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/.test(emailForm.newEmail)) {
    ElMessage.error(t('user.security.email.formatError'))
    return
  }

  codeLoading.value = true
  try {
    const res = await sendCode({ email: emailForm.newEmail }) as any
    if (res.code === 0) {
      ElMessage.success(t('user.security.email.sendSuccess'))
      // Start countdown
      countdown.value = 60
      timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    } else {
      ElMessage.error(res.message || t('user.security.email.sendError'))
    }
  } catch (e) {
    // ElMessage.error('发送验证码失败')
  } finally {
    codeLoading.value = false
  }
}

const handleUpdateEmail = async () => {
  if (!emailForm.newEmail || !emailForm.code) {
    ElMessage.error(t('user.security.email.incomplete'))
    return
  }
  emailLoading.value = true
  try {
    const res = await updateUserEmail({
      newEmail: emailForm.newEmail,
      code: emailForm.code
    }) as any
    if (res.code === 0) {
      ElMessage.success(t('user.security.email.success'))
      securityView.value = 'list' // go back
      // Reset form
      emailForm.newEmail = ''
      emailForm.code = ''
      if (timer) clearInterval(timer)
      countdown.value = 0
      fetchUserInfo() // refresh user info
    } else {
      ElMessage.error(res.message || t('user.security.email.updateError'))
    }
  } catch (e) {
    // ElMessage.error('修改失败')
  } finally {
    emailLoading.value = false
  }
}

const handleUpdatePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.checkPassword) {
    ElMessage.error(t('user.security.password.incomplete'))
    return
  }
  if (passwordForm.newPassword !== passwordForm.checkPassword) {
    ElMessage.error(t('user.security.password.mismatch'))
    return
  }
  // simple length check
  if (passwordForm.newPassword.length < 8) {
    ElMessage.warning(t('user.security.password.lengthWarning'))
  }

  passwordLoading.value = true
  try {
    const res = await updateUserPassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      checkPassword: passwordForm.checkPassword
    }) as any
    if (res.code === 0) {
      ElMessage.success(t('user.security.password.success'))
      // Redirect or logout logic
      window.location.reload()
    } else {
      ElMessage.error(res.message || t('user.security.password.updateError'))
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
    ElMessage.error(t('user.profileInfo.uploadFormatError'));
  }
  const isLt2M = file.size / 1024 / 1024 < 10;
  if (!isLt2M) {
    ElMessage.error(t('user.profileInfo.uploadSizeError'));
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
      ElMessage.success(t('user.profileInfo.uploadSuccess'));
      onSuccess(res.data);
    } else {
      ElMessage.error(t('user.profileInfo.uploadError') + (res.message || 'Unknown error'));
      onError();
    }
  } catch (error: unknown) {
    console.error(error)
    ElMessage.error(t('user.profileInfo.uploadErrorGeneric'));
    onError();
  }
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style scoped>
:deep(.el-menu-item.is-active) {
    background-color: var(--el-color-primary-light-9) !important;
    color: var(--el-color-primary) !important;
    font-weight: 500;
}
:deep(.el-menu-item) {
    height: 48px;
    line-height: 48px;
    margin-bottom: 8px;
    border-radius: 8px;
    color: var(--el-text-color-regular);
}
:deep(.el-menu-item:hover) {
    color: var(--el-text-color-primary);
    background-color: var(--el-fill-color-light);
}
</style>
