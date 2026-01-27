<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Sunny, Moon } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { resetPassword } from '~/api/sysUserController'
import { toggleLocale, getLocale } from '~/i18n'
import bgImage from '~/assets/login_bg.png'
import bgImageDark from '~/assets/login_bg_dark.png'
import logo from '~/assets/logo.svg'
import request from '~/request'
import { useDark, useToggle } from '@vueuse/core'

const { t } = useI18n()
const isDark = useDark()
const toggleDark = useToggle(isDark)

// 根据主题动态切换背景图
const currentBgImage = computed(() => isDark.value ? bgImageDark : bgImage)

const router = useRouter()
const form = reactive({
  email: '',
  code: '',
  newPassword: '',
  checkPassword: ''
})

const loading = ref(false)
const codeLoading = ref(false)
const countdown = ref(0)
let timer: any = null

const handleSendCode = async () => {
  if (!form.email) {
    ElMessage.warning(t('user.enterEmail'))
    return
  }
  if (!/^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/.test(form.email)) {
    ElMessage.warning(t('user.invalidEmail'))
    return
  }
  
  try {
    codeLoading.value = true
    await request.post('/user/code/send', null, {
      params: { email: form.email }
    })
    
    ElMessage.success(t('user.codeSent'))
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error: any) {
    ElMessage.error(error.message || t('user.sendCodeFailed'))
  } finally {
    codeLoading.value = false
  }
}

const handleReset = async () => {
  if (!form.email || !form.code || !form.newPassword || !form.checkPassword) {
    ElMessage.warning(t('user.fillAllFields'))
    return
  }
  if (form.newPassword !== form.checkPassword) {
    ElMessage.warning(t('user.passwordMismatch'))
    return
  }

  loading.value = true
  try {
    const res = await resetPassword({
      email: form.email,
      code: form.code,
      newPassword: form.newPassword,
      checkPassword: form.checkPassword
    })

    if (res.code === 0) {
      ElMessage.success(t('user.resetSuccess'))
      router.push('/common/login')
    } else {
      ElMessage.error(res.message || t('user.resetFailed'))
    }
  } catch (error: any) {
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

<route lang="yaml">
meta:
  layout: blank
</route>

<template>
  <div class="min-h-screen w-full flex bg-white dark:bg-gray-900">
    <!-- 左侧：背景图 -->
    <div class="hidden md:flex w-1/2 relative bg-cover bg-center" :style="{ backgroundImage: `url(${currentBgImage})` }">
      <!-- 遮罩层 -->
      <div class="absolute inset-0 bg-blue-900/30 dark:bg-gray-900/60 backdrop-blur-[2px]" />
      <!-- 左上角：Logo -->
      <div class="absolute top-8 left-8 flex items-center gap-5 text-white z-10">
        <div class="w-16 h-16 bg-white/20 dark:bg-white/10 backdrop-blur-md rounded-xl flex items-center justify-center border border-white/30 dark:border-white/20">
          <img :src="logo" alt="Logo" class="w-10 h-10" />
        </div>
        <span class="text-3xl font-bold tracking-wide text-white">PaperInsight</span>
      </div>

      <!-- 中间内容 -->
      <div class="absolute top-1/2 left-0 right-0 transform -translate-y-1/2 px-12 z-10">
        <h2 class="text-5xl font-serif font-bold leading-tight mb-6 tracking-wide text-white">
          {{ $t('home.heroTitle1') }}<br />{{ $t('home.heroTitle2') }}
        </h2>
        <div class="w-12 h-1 bg-white/50 dark:bg-white/30 mb-6" />
        <p class="text-xl text-white/90 font-light tracking-widest mb-10">
          {{ $t('home.heroSlogan') }}
        </p>

        <!-- 特性标签 -->
        <div class="flex gap-4">
          <div class="px-4 py-2 rounded-full bg-white/10 dark:bg-white/5 backdrop-blur-md border border-white/20 dark:border-white/10 flex items-center gap-2 text-sm font-light text-white">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-400" />
            {{ $t('home.featureTools') }}
          </div>
          <div class="px-4 py-2 rounded-full bg-white/10 dark:bg-white/5 backdrop-blur-md border border-white/20 dark:border-white/10 flex items-center gap-2 text-sm font-light text-white">
            <span class="w-1.5 h-1.5 rounded-full bg-blue-400" />
            {{ $t('home.featureCollab') }}
          </div>
        </div>
      </div>

      <!-- 底部引用 -->
      <div class="absolute bottom-10 left-12 text-white/60 dark:text-white/40 text-sm font-serif italic tracking-wider z-10">
        " {{ $t('home.registerQuote') }} "
      </div>
    </div>

    <!-- 右侧：重置密码表单 -->
    <div class="w-full md:w-1/2 flex items-center justify-center p-8 md:p-16 relative">
      <!-- 主题/语言切换按钮 -->
      <div class="absolute top-6 right-6 flex gap-2">
        <button
          @click="toggleDark()"
          class="w-9 h-9 flex items-center justify-center rounded-lg bg-gray-100 dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors"
          :title="isDark ? 'Light Mode' : 'Dark Mode'"
        >
          <Moon v-if="!isDark" class="w-5 h-5 text-gray-600" />
          <Sunny v-else class="w-5 h-5 text-yellow-500" />
        </button>
        <button
          @click="toggleLocale()"
          class="w-9 h-9 flex items-center justify-center rounded-lg bg-gray-100 dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700 text-sm font-medium transition-colors"
        >
          {{ getLocale() === 'zh' ? 'EN' : '中' }}
        </button>
      </div>
      <div class="w-full max-w-[420px]">
        <!-- 标题 -->
        <div class="mb-10">
          <h1 class="text-3xl font-serif text-gray-900 dark:text-white mb-3 tracking-wide">{{ $t('user.resetPasswordTitle') }}</h1>
          <p class="text-gray-500 text-sm">{{ $t('user.resetPasswordSubtitle') }}</p>
        </div>

        <!-- 表单 -->
        <div class="space-y-5">
          <el-input 
            v-model="form.email" 
            :placeholder="$t('user.emailPlaceholder')" 
            size="large"
            class="custom-input h-12"
          >
            <template #prefix>
              <div class="i-ep-message" />
            </template>
          </el-input>
          <div class="flex gap-4">
            <el-input 
              v-model="form.code" 
              :placeholder="$t('user.codePlaceholder')" 
              size="large"
              class="custom-input flex-1 h-12"
            >
              <template #prefix>
                <div class="i-ep-key" />
              </template>
            </el-input>
            <el-button 
              type="primary" 
              size="large" 
              class="w-32 h-12 !ml-0"
              :disabled="countdown > 0" 
              :loading="codeLoading"
              @click="handleSendCode"
            >
              {{ countdown > 0 ? `${countdown}s` : $t('user.sendCode') }}
            </el-button>
          </div>
          <el-input 
            v-model="form.newPassword" 
            :placeholder="$t('user.newPasswordPlaceholder')" 
            type="password" 
            size="large"
            show-password
            class="custom-input h-12"
          >
            <template #prefix>
              <div class="i-ep-lock" />
            </template>
          </el-input>
          <el-input 
            v-model="form.checkPassword" 
            :placeholder="$t('user.confirmNewPasswordPlaceholder')" 
            type="password" 
            size="large"
            show-password
            class="custom-input h-12"
          >
            <template #prefix>
              <div class="i-ep-lock" />
            </template>
          </el-input>
        </div>

        <!-- 底部链接 -->
        <div class="flex justify-between items-center text-sm mt-6 mb-8">
          <router-link to="/common/login" class="font-medium text-blue-600 hover:underline transition-colors">
            {{ $t('user.backToLogin') }}
          </router-link>
        </div>

        <!-- 提交按钮 -->
        <el-button 
          type="primary" 
          size="large" 
          class="w-full h-12 text-lg font-medium tracking-wide shadow-lg shadow-blue-500/20 hover:shadow-blue-500/30 transition-all" 
          :loading="loading"
          @click="handleReset"
        >
          {{ $t('user.confirmReset') }}
        </el-button>

        <!-- 底部版权 -->
        <div class="text-center mt-10 text-xs text-gray-400 font-light tracking-wider">
          © 2026 PaperInsight · {{ $t('home.copyright') }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.el-input__wrapper) {
  box-shadow: none !important;
  background-color: #f3f4f6 !important;
  border-radius: 0.5rem;
  transition: all 0.3s;
}

:deep(.el-input__inner) {
  color: #111827 !important;
}

:deep(.el-input__wrapper:hover) {
  background-color: #e5e7eb !important;
}

:deep(.el-input__wrapper.is-focus) {
  background-color: #fff;
  box-shadow: 0 0 0 1px var(--el-color-primary) !important;
  background-color: #fff !important;
}
</style>
