<script setup lang="ts">
import { Key, Lock, Message, Moon, Sunny, User } from '@element-plus/icons-vue'
import { useDark, useToggle } from '@vueuse/core'
import { ElMessage } from 'element-plus'
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { userLogin } from '~/api/sysUserController'
import bgImage from '~/assets/login_bg.png'
import bgImageDark from '~/assets/login_bg_dark.png'
import logo from '~/assets/logo.svg'
import { getLocale, toggleLocale } from '~/i18n'
import request from '~/request'
import { useUserStore } from '~/stores/user'

const { t } = useI18n()
const isDark = useDark()
const toggleDark = useToggle(isDark)

// 根据主题动态切换背景图
const currentBgImage = computed(() => isDark.value ? bgImageDark : bgImage)

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const activeTab = ref('account')

// 获取重定向地址
function getRedirectPath() {
  const redirect = route.query.redirect as string
  if (redirect) {
    try {
      const decodedPath = decodeURIComponent(redirect)
      // 确保路径以 / 开头
      return decodedPath.startsWith('/') ? decodedPath : `/${decodedPath}`
    }
    catch (e) {
      // 解码失败，使用原始值
      return redirect.startsWith('/') ? redirect : `/${redirect}`
    }
  }
  return '/'
}

// 账号登录表单
const accountForm = reactive({
  userAccount: '',
  userPassword: '',
  type: 'account',
})

// 邮箱登录表单
const emailForm = reactive({
  email: '',
  code: '',
  type: 'email',
})

const loading = ref(false)
const codeLoading = ref(false)
const countdown = ref(0)
let timer: any = null

async function handleSendCode() {
  if (!emailForm.email) {
    ElMessage.warning(t('user.enterEmail'))
    return
  }
  if (!/^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/.test(emailForm.email)) {
    ElMessage.warning(t('user.invalidEmail'))
    return
  }

  try {
    codeLoading.value = true
    await request.post('/user/code/send', null, {
      params: { email: emailForm.email },
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

async function handleLogin() {
  loading.value = true
  try {
    let res
    if (activeTab.value === 'account') {
      if (!accountForm.userAccount || !accountForm.userPassword) {
        ElMessage.warning(t('user.fillAllFields'))
        return
      }
      res = await userLogin({
        userAccount: accountForm.userAccount,
        userPassword: accountForm.userPassword,
        type: 'account',
      })
    }
    else {
      if (!emailForm.email || !emailForm.code) {
        ElMessage.warning(t('user.fillAllFields'))
        return
      }
      res = await userLogin({
        email: emailForm.email,
        code: emailForm.code,
        type: 'email',
      })
    }

    if (res.code === 0 && res.data) {
      userStore.setLoginUser(res.data)
      ElMessage.success(t('user.loginSuccess'))
      // 跳转到重定向地址或首页
      const redirectPath = getRedirectPath()
      router.push(redirectPath)
    }
    else {
      ElMessage.error(res.message || t('user.loginFailed'))
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
        <div class="mb-8 h-1.5 w-16 bg-white/50 dark:bg-white/30" />
        <p class="mb-12 text-2xl text-white/90 font-light tracking-widest">
          {{ $t('home.heroSlogan') }}
        </p>

        <!-- 特性标签 -->
        <div class="flex gap-4">
          <div class="flex items-center gap-2 border border-white/20 rounded-full bg-white/10 px-4 py-2 text-sm text-white font-light backdrop-blur-md dark:border-white/10 dark:bg-white/5">
            <span class="h-1.5 w-1.5 rounded-full bg-emerald-400" />
            {{ $t('home.featureAI') }}
          </div>
          <div class="flex items-center gap-2 border border-white/20 rounded-full bg-white/10 px-4 py-2 text-sm text-white font-light backdrop-blur-md dark:border-white/10 dark:bg-white/5">
            <span class="h-1.5 w-1.5 rounded-full bg-blue-400" />
            {{ $t('home.featureParsing') }}
          </div>
        </div>
      </div>

      <!-- 底部引用 -->
      <div class="absolute bottom-10 left-12 z-10 text-sm text-white/60 tracking-wider font-serif italic dark:text-white/40">
        " {{ $t('home.heroQuote') }} "
      </div>
    </div>

    <!-- 右侧：登录表单 -->
    <div class="relative w-full flex items-center justify-center bg-white p-8 md:w-1/2 dark:bg-gray-900 md:p-16">
      <!-- 右上角：主题和语言切换 -->
      <div class="absolute right-6 top-6 flex items-center gap-3">
        <button
          class="h-10 w-10 flex items-center justify-center rounded-full bg-gray-100 transition-colors dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600"
          :title="isDark ? 'Light Mode' : 'Dark Mode'"
          @click="toggleDark()"
        >
          <el-icon :size="18" class="text-gray-600 dark:text-gray-300">
            <Moon v-if="!isDark" />
            <Sunny v-else />
          </el-icon>
        </button>
        <button
          class="h-10 w-10 flex items-center justify-center rounded-full bg-gray-100 text-sm text-gray-600 font-medium transition-colors dark:bg-gray-700 hover:bg-gray-200 dark:text-gray-300 dark:hover:bg-gray-600"
          @click="toggleLocale()"
        >
          {{ getLocale() === 'zh' ? 'EN' : '中' }}
        </button>
      </div>
      <div class="max-w-[420px] w-full">
        <!-- 标题 -->
        <div class="mb-10">
          <h1 class="mb-3 text-3xl text-gray-900 tracking-wide font-serif dark:text-white">
            {{ $t('user.welcomeBack') }}
          </h1>
          <p class="text-sm text-gray-500">
            {{ $t('user.loginSubtitle') }}
          </p>
        </div>

        <!-- 切换 Tab -->
        <!-- 切换 Tab (胶囊样式，无黑边) -->
        <div class="grid grid-cols-2 mb-10 gap-1 rounded-xl bg-gray-50 p-1 dark:bg-gray-800">
          <button
            class="rounded-lg py-2.5 text-sm font-medium transition-all duration-200"
            :class="activeTab === 'account' ? 'bg-white dark:bg-gray-700 text-gray-900 dark:text-white shadow-sm' : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            @click="activeTab = 'account'"
          >
            {{ $t('user.accountLogin') }}
          </button>
          <button
            class="rounded-lg py-2.5 text-sm font-medium transition-all duration-200"
            :class="activeTab === 'email' ? 'bg-white dark:bg-gray-700 text-gray-900 dark:text-white shadow-sm' : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            @click="activeTab = 'email'"
          >
            {{ $t('user.emailLogin') }}
          </button>
        </div>

        <!-- 账号表单 -->
        <div v-if="activeTab === 'account'" class="space-y-6">
          <el-input
            v-model="accountForm.userAccount"
            :placeholder="$t('user.accountPlaceholder')"
            :prefix-icon="User"
            size="large"
            class="custom-input h-12"
          />
          <el-input
            v-model="accountForm.userPassword"
            :placeholder="$t('user.passwordPlaceholder')"
            type="password"
            :prefix-icon="Lock"
            size="large"
            show-password
            class="custom-input h-12"
            @keyup.enter="handleLogin"
          />
        </div>

        <!-- 邮箱表单 -->
        <div v-else class="space-y-6">
          <el-input
            v-model="emailForm.email"
            :placeholder="$t('user.emailPlaceholder')"
            :prefix-icon="Message"
            size="large"
            class="custom-input h-12"
          />
          <div class="flex gap-4">
            <el-input
              v-model="emailForm.code"
              :placeholder="$t('user.codePlaceholder')"
              :prefix-icon="Key"
              size="large"
              class="custom-input h-12 flex-1"
              @keyup.enter="handleLogin"
            />
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
        </div>

        <!-- 底部链接 -->
        <div class="mb-8 mt-6 flex items-center justify-between text-sm">
          <router-link to="/common/forgot-password" class="text-blue-600 transition-colors hover:underline">
            {{ $t('user.forgotPassword') }}
          </router-link>
          <router-link to="/common/register" class="text-blue-600 font-medium transition-colors hover:underline">
            {{ $t('user.goRegister') }}
          </router-link>
        </div>

        <!-- 登录按钮 -->
        <el-button
          type="primary"
          size="large"
          class="h-12 w-full text-lg font-medium tracking-wide shadow-blue-500/20 shadow-lg transition-all hover:shadow-blue-500/30"
          :loading="loading"
          @click="handleLogin"
        >
          {{ $t('user.loginButton') }}
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
  background-color: #f3f4f6 !important; /* 强制浅灰背景 */
  border-radius: 0.5rem;
  transition: all 0.3s;
}

:deep(.el-input__inner) {
  color: #111827 !important; /* 强制黑色文字 */
}

:deep(.el-input__wrapper:hover) {
  background-color: #e5e7eb !important;
  box-shadow: none !important;
}

:deep(.el-input__wrapper.is-focus) {
  background-color: #fff;
  box-shadow: 0 0 0 1px var(--el-color-primary) !important;
  background-color: #fff !important;
}
</style>
