<script setup lang="ts">
import { Moon, Sunny } from '@element-plus/icons-vue'
import { useDark, useToggle } from '@vueuse/core'
import { ElMessage } from 'element-plus'
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { resetPassword } from '~/api/sysUserController'
import bgImage from '~/assets/login_bg.png'
import bgImageDark from '~/assets/login_bg_dark.png'
import logo from '~/assets/logo.svg'
import { getLocale, toggleLocale } from '~/i18n'
import request from '~/request'

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
  checkPassword: '',
})

const loading = ref(false)
const codeLoading = ref(false)
const countdown = ref(0)
let timer: any = null

async function handleSendCode() {
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
      params: { email: form.email },
    })

    ElMessage.success(t('user.codeSent'))
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  }
  catch (error: any) {
    ElMessage.error(error.message || t('user.sendCodeFailed'))
  }
  finally {
    codeLoading.value = false
  }
}

async function handleReset() {
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
      checkPassword: form.checkPassword,
    })

    if (res.code === 0) {
      ElMessage.success(t('user.resetSuccess'))
      router.push('/common/login')
    }
    else {
      ElMessage.error(res.message || t('user.resetFailed'))
    }
  }
  catch (error: any) {
    console.error(error)
  }
  finally {
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
    <div class="relative hidden w-1/2 bg-cover bg-center md:flex" :style="{ backgroundImage: `url(${currentBgImage})` }">
      <!-- 遮罩层 -->
      <div class="absolute inset-0 bg-blue-900/30 backdrop-blur-[2px] dark:bg-gray-900/60" />
      <!-- 左上角：Logo -->
      <div class="absolute left-8 top-8 z-10 flex items-center gap-5 text-white">
        <div class="h-16 w-16 flex items-center justify-center border border-white/30 rounded-xl bg-white/20 backdrop-blur-md dark:border-white/20 dark:bg-white/10">
          <img :src="logo" alt="Logo" class="h-10 w-10">
        </div>
        <span class="text-3xl text-white font-bold tracking-wide">PaperInsight</span>
      </div>

      <!-- 中间内容 -->
      <div class="absolute left-0 right-0 top-1/2 z-10 transform px-12 -translate-y-1/2">
        <h2 class="mb-6 text-5xl text-white font-bold leading-tight tracking-wide font-serif">
          {{ $t('home.heroTitle1') }}<br>{{ $t('home.heroTitle2') }}
        </h2>
        <div class="mb-6 h-1 w-12 bg-white/50 dark:bg-white/30" />
        <p class="mb-10 text-xl text-white/90 font-light tracking-widest">
          {{ $t('home.heroSlogan') }}
        </p>

        <!-- 特性标签 -->
        <div class="flex gap-4">
          <div class="flex items-center gap-2 border border-white/20 rounded-full bg-white/10 px-4 py-2 text-sm text-white font-light backdrop-blur-md dark:border-white/10 dark:bg-white/5">
            <span class="h-1.5 w-1.5 rounded-full bg-emerald-400" />
            {{ $t('home.featureTools') }}
          </div>
          <div class="flex items-center gap-2 border border-white/20 rounded-full bg-white/10 px-4 py-2 text-sm text-white font-light backdrop-blur-md dark:border-white/10 dark:bg-white/5">
            <span class="h-1.5 w-1.5 rounded-full bg-blue-400" />
            {{ $t('home.featureCollab') }}
          </div>
        </div>
      </div>

      <!-- 底部引用 -->
      <div class="absolute bottom-10 left-12 z-10 text-sm text-white/60 tracking-wider font-serif italic dark:text-white/40">
        " {{ $t('home.registerQuote') }} "
      </div>
    </div>

    <!-- 右侧：重置密码表单 -->
    <div class="relative w-full flex items-center justify-center p-8 md:w-1/2 md:p-16">
      <!-- 主题/语言切换按钮 -->
      <div class="absolute right-6 top-6 flex gap-2">
        <button
          class="h-9 w-9 flex items-center justify-center rounded-lg bg-gray-100 transition-colors dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700"
          :title="isDark ? 'Light Mode' : 'Dark Mode'"
          @click="toggleDark()"
        >
          <Moon v-if="!isDark" class="h-5 w-5 text-gray-600" />
          <Sunny v-else class="h-5 w-5 text-yellow-500" />
        </button>
        <button
          class="h-9 w-9 flex items-center justify-center rounded-lg bg-gray-100 text-sm font-medium transition-colors dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700"
          @click="toggleLocale()"
        >
          {{ getLocale() === 'zh' ? 'EN' : '中' }}
        </button>
      </div>
      <div class="max-w-[420px] w-full">
        <!-- 标题 -->
        <div class="mb-10">
          <h1 class="mb-3 text-3xl text-gray-900 tracking-wide font-serif dark:text-white">
            {{ $t('user.resetPasswordTitle') }}
          </h1>
          <p class="text-sm text-gray-500">
            {{ $t('user.resetPasswordSubtitle') }}
          </p>
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
              class="custom-input h-12 flex-1"
            >
              <template #prefix>
                <div class="i-ep-key" />
              </template>
            </el-input>
            <el-button
              type="primary"
              size="large"
              class="h-12 w-32 !ml-0"
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
        <div class="mb-8 mt-6 flex items-center justify-between text-sm">
          <router-link to="/common/login" class="text-blue-600 font-medium transition-colors hover:underline">
            {{ $t('user.backToLogin') }}
          </router-link>
        </div>

        <!-- 提交按钮 -->
        <el-button
          type="primary"
          size="large"
          class="h-12 w-full text-lg font-medium tracking-wide shadow-blue-500/20 shadow-lg transition-all hover:shadow-blue-500/30"
          :loading="loading"
          @click="handleReset"
        >
          {{ $t('user.confirmReset') }}
        </el-button>

        <!-- 底部版权 -->
        <div class="mt-10 text-center text-xs text-gray-400 font-light tracking-wider">
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
