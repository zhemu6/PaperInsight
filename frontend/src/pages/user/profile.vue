<script setup lang="ts">
import {
  ArrowLeft,
  ArrowRight,
  Lock,
  Message,
  Setting,
  User,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { uploadFile } from '~/api/fileController'
import { getLoginUser, sendCode, updateMyInfo, updateUserEmail, updateUserPassword } from '~/api/sysUserController'

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
  code: '',
})
const emailLoading = ref(false)
const codeLoading = ref(false)
const countdown = ref(0)
let timer: any = null

// Update Password Form
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  checkPassword: '',
})
const passwordLoading = ref(false)

function handleMenuSelect(key: string) {
  activeTab.value = key
}

// Watch tab change
watch(activeTab, (val: string) => {
  if (val === 'security') {
    securityView.value = 'list' // reset to list view
  }
})

async function fetchUserInfo() {
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
  }
  catch (error) {
    console.error(error)
  }
}

function handleReset() {
  formState.userName = currentUser.value?.userName || ''
  formState.userAvatar = currentUser.value?.userAvatar || ''
  formState.userProfile = currentUser.value?.userProfile || ''
  ElMessage.info(t('user.profileInfo.resetMessage'))
}

async function handleSave() {
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
      userProfile: formState.userProfile,
    }) as any

    if (res.code === 0) {
      ElMessage.success(t('user.profileInfo.saveSuccess'))
      await fetchUserInfo()
    }
    else {
      ElMessage.error(res.message || t('user.profileInfo.saveFailed'))
    }
  }
  catch (error: unknown) {
    console.error(error)
  }
  finally {
    loading.value = false
  }
}

// Logic for sending email code
async function handleSendCode() {
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
    }
    else {
      ElMessage.error(res.message || t('user.security.email.sendError'))
    }
  }
  catch (e) {
    // ElMessage.error('发送验证码失败')
  }
  finally {
    codeLoading.value = false
  }
}

async function handleUpdateEmail() {
  if (!emailForm.newEmail || !emailForm.code) {
    ElMessage.error(t('user.security.email.incomplete'))
    return
  }
  emailLoading.value = true
  try {
    const res = await updateUserEmail({
      newEmail: emailForm.newEmail,
      code: emailForm.code,
    }) as any
    if (res.code === 0) {
      ElMessage.success(t('user.security.email.success'))
      securityView.value = 'list' // go back
      // Reset form
      emailForm.newEmail = ''
      emailForm.code = ''
      if (timer)
        clearInterval(timer)
      countdown.value = 0
      fetchUserInfo() // refresh user info
    }
    else {
      ElMessage.error(res.message || t('user.security.email.updateError'))
    }
  }
  catch (e) {
    // ElMessage.error('修改失败')
  }
  finally {
    emailLoading.value = false
  }
}

async function handleUpdatePassword() {
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
      checkPassword: passwordForm.checkPassword,
    }) as any
    if (res.code === 0) {
      ElMessage.success(t('user.security.password.success'))
      // Redirect or logout logic
      window.location.reload()
    }
    else {
      ElMessage.error(res.message || t('user.security.password.updateError'))
    }
  }
  catch (e) {
    // ElMessage.error('修改失败')
  }
  finally {
    passwordLoading.value = false
  }
}

// 图像上传逻辑
function beforeUpload(file: File) {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/webp'
  if (!isJpgOrPng) {
    ElMessage.error(t('user.profileInfo.uploadFormatError'))
  }
  const isLt2M = file.size / 1024 / 1024 < 10
  if (!isLt2M) {
    ElMessage.error(t('user.profileInfo.uploadSizeError'))
  }
  return isJpgOrPng && isLt2M
}

async function handleUpload(options: any) {
  const { file, onSuccess, onError } = options
  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await uploadFile(formData) as any
    if (res.code === 0) {
      formState.userAvatar = res.data || ''
      // Also update backend user record immediately or wait for save?
      // Typically avatar upload is independent or just gets the URL.
      // Here we just set the URL and user needs to click "Save Changes".
      ElMessage.success(t('user.profileInfo.uploadSuccess'))
      onSuccess(res.data)
    }
    else {
      ElMessage.error(t('user.profileInfo.uploadError') + (res.message || 'Unknown error'))
      onError()
    }
  }
  catch (error: unknown) {
    console.error(error)
    ElMessage.error(t('user.profileInfo.uploadErrorGeneric'))
    onError()
  }
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<template>
  <div class="settings-page mx-auto max-w-screen-xl">
    <!-- 顶部标题 -->
    <div class="mb-8">
      <h1 class="mb-2 text-2xl text-[var(--el-text-color-primary)] font-bold">
        {{ t('user.profileInfo.editTitle') }}
      </h1>
      <p class="text-[var(--el-text-color-secondary)]">
        {{ t('user.profileInfo.editSubtitle') }}
      </p>
    </div>

    <div class="flex flex-col gap-6 md:flex-row">
      <!-- 左侧导航 -->
      <div class="w-full flex-shrink-0 md:w-64">
        <el-card shadow="never" class="shadow-sm !rounded-xl !border-none">
          <el-menu
            :default-active="activeTab"
            class="w-full !border-r-0 !border-none"
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
        <el-card shadow="never" class="min-h-[600px] shadow-sm !rounded-xl !border-none">
          <!-- 个人信息面板 -->
          <div v-if="activeTab === 'profile'" class="p-4">
            <h2 class="mb-8 text-lg text-[var(--el-text-color-primary)] font-medium">
              {{ t('user.profileInfo.basicTitle') }}
            </h2>

            <el-form :model="formState" label-position="top" class="max-w-3xl">
              <div class="mb-8">
                <h3 class="mb-4 text-sm text-[var(--el-text-color-secondary)] font-medium">
                  {{ t('user.profileInfo.basicSubtitle') }}
                </h3>
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
                <h3 class="mb-4 text-sm text-[var(--el-text-color-secondary)] font-medium">
                  {{ t('user.profileInfo.avatarLabel') }}
                </h3>
                <div class="flex items-center gap-6">
                  <el-avatar :size="80" :src="formState.userAvatar" class="flex-shrink-0 bg-[var(--el-color-primary-light-9)] text-2xl text-[var(--el-color-primary)]">
                    {{ formState.userName?.charAt(0)?.toUpperCase() }}
                  </el-avatar>
                  <div>
                    <el-upload
                      name="file"
                      :show-file-list="false"
                      :http-request="handleUpload"
                      :before-upload="beforeUpload"
                    >
                      <el-button type="primary" link class="cursor-pointer font-medium">
                        {{ t('user.profileInfo.uploadText') }}
                      </el-button>
                    </el-upload>
                    <p class="mt-1 text-xs text-[var(--el-text-color-placeholder)]">
                      {{ t('user.profileInfo.uploadHint') }}
                    </p>
                  </div>
                </div>
              </div>

              <div class="mt-12 flex justify-end gap-3 border-t border-[var(--el-border-color-lighter)] pt-6">
                <el-button size="large" @click="handleReset">
                  {{ t('user.profileInfo.cancel') }}
                </el-button>
                <el-button type="primary" size="large" :loading="loading" @click="handleSave">
                  {{ t('user.profileInfo.save') }}
                </el-button>
              </div>
            </el-form>
          </div>

          <!-- 安全设置面板 -->
          <div v-if="activeTab === 'security'" class="p-4">
            <!-- Security List View -->
            <div v-if="securityView === 'list'">
              <!-- Account Security -->
              <div class="mb-10">
                <h3 class="mb-4 text-base text-[var(--el-text-color-primary)] font-bold">
                  {{ t('user.security.title') }}
                </h3>
                <p class="mb-6 text-sm text-[var(--el-text-color-secondary)]">
                  {{ t('user.security.subtitle') }}
                </p>

                <div class="space-y-4">
                  <!-- Email -->
                  <div class="flex items-center justify-between border border-[var(--el-border-color-lighter)] rounded-lg p-6 transition-colors hover:border-[var(--el-color-primary-light-5)]">
                    <div class="flex items-center gap-4">
                      <div class="h-10 w-10 flex items-center justify-center rounded-full bg-[var(--el-color-primary-light-9)] text-xl text-[var(--el-color-primary)]">
                        <el-icon><Message /></el-icon>
                      </div>
                      <div>
                        <div class="text-base text-[var(--el-text-color-regular)] font-medium">
                          {{ t('user.security.email.label') }}
                        </div>
                        <div class="mt-1 text-sm text-[var(--el-text-color-secondary)]">
                          {{ currentUser.email || t('user.security.email.notBound') }}
                        </div>
                      </div>
                    </div>
                    <el-button type="primary" link class="!text-[var(--el-text-color-secondary)] hover:!text-[var(--el-color-primary)]" @click="securityView = 'email'">
                      {{ t('user.security.email.change') }} <el-icon class="ml-1">
                        <ArrowRight />
                      </el-icon>
                    </el-button>
                  </div>

                  <!-- Password -->
                  <div class="flex items-center justify-between border border-[var(--el-border-color-lighter)] rounded-lg p-6 transition-colors hover:border-[var(--el-color-primary-light-5)]">
                    <div class="flex items-center gap-4">
                      <div class="h-10 w-10 flex items-center justify-center rounded-full bg-[var(--el-color-primary-light-9)] text-xl text-[var(--el-color-primary)]">
                        <el-icon><Lock /></el-icon>
                      </div>
                      <div>
                        <div class="text-base text-[var(--el-text-color-regular)] font-medium">
                          {{ t('user.security.password.label') }}
                        </div>
                        <div class="mt-1 text-sm text-[var(--el-text-color-secondary)]">
                          {{ t('user.security.password.desc') }}
                        </div>
                      </div>
                    </div>
                    <el-button type="primary" link class="!text-[var(--el-text-color-secondary)] hover:!text-[var(--el-color-primary)]" @click="securityView = 'password'">
                      {{ t('user.security.password.change') }} <el-icon class="ml-1">
                        <ArrowRight />
                      </el-icon>
                    </el-button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Update Email View -->
            <div v-if="securityView === 'email'">
              <div class="mb-8 flex cursor-pointer items-center gap-2 text-[var(--el-text-color-secondary)] hover:text-[var(--el-color-primary)]" @click="securityView = 'list'">
                <el-icon><ArrowLeft /></el-icon> {{ t('user.security.back') }}
              </div>
              <h2 class="mb-2 text-xl text-[var(--el-text-color-primary)] font-bold">
                {{ t('user.security.email.title') }}
              </h2>
              <p class="mb-8 text-[var(--el-text-color-secondary)]">
                {{ t('user.security.email.subtitle') }}
              </p>

              <el-alert :title="t('user.security.email.warning')" type="info" show-icon class="mb-8" :closable="false" />

              <el-form label-position="top" class="max-w-lg">
                <el-form-item :label="t('user.security.email.current')">
                  <el-input size="large" :value="currentUser.email" disabled class="bg-gray-50" />
                </el-form-item>

                <el-form-item :label="t('user.security.email.new')" required>
                  <el-input v-model="emailForm.newEmail" size="large" :placeholder="t('user.security.email.newPlaceholder')" />
                </el-form-item>

                <el-form-item :label="t('user.security.email.code')" required>
                  <div class="w-full flex gap-4">
                    <el-input v-model="emailForm.code" size="large" :placeholder="t('user.security.email.codePlaceholder')" />
                    <el-button size="large" :loading="codeLoading" :disabled="countdown > 0" @click="handleSendCode">
                      {{ countdown > 0 ? `${countdown}${t('user.security.email.retry')}` : t('user.security.email.sendCode') }}
                    </el-button>
                  </div>
                </el-form-item>

                <div class="mt-8 flex justify-end gap-3">
                  <el-button size="large" link @click="securityView = 'list'">
                    {{ t('user.security.email.reset') }}
                  </el-button>
                  <el-button type="primary" size="large" :loading="emailLoading" @click="handleUpdateEmail">
                    {{ t('user.security.email.confirm') }}
                  </el-button>
                </div>
              </el-form>
            </div>

            <!-- Update Password View -->
            <div v-if="securityView === 'password'">
              <div class="mb-8 flex cursor-pointer items-center gap-2 text-[var(--el-text-color-secondary)] hover:text-[var(--el-color-primary)]" @click="securityView = 'list'">
                <el-icon><ArrowLeft /></el-icon> {{ t('user.security.back') }}
              </div>
              <h2 class="mb-2 text-xl text-[var(--el-text-color-primary)] font-bold">
                {{ t('user.security.password.title') }}
              </h2>
              <p class="mb-8 text-[var(--el-text-color-secondary)]">
                {{ t('user.security.password.subtitle') }}
              </p>

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

                <div class="mt-8 flex justify-end gap-3">
                  <el-button size="large" link @click="securityView = 'list'">
                    {{ t('user.security.password.reset') }}
                  </el-button>
                  <el-button type="primary" size="large" :loading="passwordLoading" @click="handleUpdatePassword">
                    {{ t('user.security.password.update') }}
                  </el-button>
                </div>
              </el-form>
            </div>
          </div>

          <!-- 偏好设置面板 -->
          <div v-if="activeTab === 'preference'" class="h-[400px] flex items-center justify-center p-4 text-[var(--el-text-color-placeholder)]">
            {{ t('user.preference.developing') }}
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

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
