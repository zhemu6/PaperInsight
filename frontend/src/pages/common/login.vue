<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { User, Lock, Message, Key, Sunny, Moon } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { userLogin } from '~/api/sysUserController'
import { useUserStore } from '~/stores/user'
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


const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const activeTab = ref('account')

// 获取重定向地址
const getRedirectPath = () => {
  const redirect = route.query.redirect as string
  if (redirect) {
    try {
      const decodedPath = decodeURIComponent(redirect)
      // 确保路径以 / 开头
      return decodedPath.startsWith('/') ? decodedPath : `/${decodedPath}`
    } catch (e) {
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
  type: 'account'
})

// 邮箱登录表单
const emailForm = reactive({
  email: '',
  code: '',
  type: 'email'
})

const loading = ref(false)
const codeLoading = ref(false)
const countdown = ref(0)
let timer: any = null

const handleSendCode = async () => {
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
      params: { email: emailForm.email }
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

const handleLogin = async () => {
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
        type: 'account'
      })
    } else {
      if (!emailForm.email || !emailForm.code) {
        ElMessage.warning(t('user.fillAllFields'))
        return
      }
      res = await userLogin({
        email: emailForm.email,
        code: emailForm.code,
        type: 'email'
      })
    }

    if (res.code === 0 && res.data) {
      userStore.setLoginUser(res.data)
      ElMessage.success(t('user.loginSuccess'))
      // 跳转到重定向地址或首页
      const redirectPath = getRedirectPath()
      router.push(redirectPath)
    } else {
      ElMessage.error(res.message || t('user.loginFailed'))
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
        <div class="w-16 h-1.5 bg-white/50 dark:bg-white/30 mb-8" />
        <p class="text-2xl text-white/90 font-light tracking-widest mb-12">
          {{ $t('home.heroSlogan') }}
        </p>

        <!-- 特性标签 -->
        <div class="flex gap-4">
          <div class="px-4 py-2 rounded-full bg-white/10 dark:bg-white/5 backdrop-blur-md border border-white/20 dark:border-white/10 flex items-center gap-2 text-sm font-light text-white">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-400" />
            {{ $t('home.featureAI') }}
          </div>
          <div class="px-4 py-2 rounded-full bg-white/10 dark:bg-white/5 backdrop-blur-md border border-white/20 dark:border-white/10 flex items-center gap-2 text-sm font-light text-white">
            <span class="w-1.5 h-1.5 rounded-full bg-blue-400" />
            {{ $t('home.featureParsing') }}
          </div>
        </div>
      </div>

      <!-- 底部引用 -->
      <div class="absolute bottom-10 left-12 text-white/60 dark:text-white/40 text-sm font-serif italic tracking-wider z-10">
        " {{ $t('home.heroQuote') }} "
      </div>
    </div>

    <!-- 右侧：登录表单 -->
    <div class="w-full md:w-1/2 flex items-center justify-center p-8 md:p-16 relative bg-white dark:bg-gray-900">
      <!-- 右上角：主题和语言切换 -->
      <div class="absolute top-6 right-6 flex items-center gap-3">
        <button 
          @click="toggleDark()"
          class="w-10 h-10 rounded-full flex items-center justify-center bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
          :title="isDark ? 'Light Mode' : 'Dark Mode'"
        >
          <el-icon :size="18" class="text-gray-600 dark:text-gray-300">
            <Moon v-if="!isDark" />
            <Sunny v-else />
          </el-icon>
        </button>
        <button 
          @click="toggleLocale()"
          class="w-10 h-10 rounded-full flex items-center justify-center bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors text-sm font-medium text-gray-600 dark:text-gray-300"
        >
          {{ getLocale() === 'zh' ? 'EN' : '中' }}
        </button>
      </div>
      <div class="w-full max-w-[420px]">
        <!-- 标题 -->
        <div class="mb-10">
          <h1 class="text-3xl font-serif text-gray-900 dark:text-white mb-3 tracking-wide">{{ $t('user.welcomeBack') }}</h1>
          <p class="text-gray-500 text-sm">{{ $t('user.loginSubtitle') }}</p>
        </div>

        <!-- 切换 Tab -->
        <!-- 切换 Tab (胶囊样式，无黑边) -->
        <div class="grid grid-cols-2 gap-1 p-1 mb-10 bg-gray-50 dark:bg-gray-800 rounded-xl">
          <button
            @click="activeTab='account'"
            class="py-2.5 text-sm font-medium rounded-lg transition-all duration-200"
            :class="activeTab==='account' ? 'bg-white dark:bg-gray-700 text-gray-900 dark:text-white shadow-sm' : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
          >
            {{ $t('user.accountLogin') }}
          </button>
          <button
            @click="activeTab='email'"
            class="py-2.5 text-sm font-medium rounded-lg transition-all duration-200"
            :class="activeTab==='email' ? 'bg-white dark:bg-gray-700 text-gray-900 dark:text-white shadow-sm' : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
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
              class="custom-input flex-1 h-12"
              @keyup.enter="handleLogin"
            />
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
        </div>

        <!-- 底部链接 -->
        <div class="flex justify-between items-center text-sm mt-6 mb-8">
          <router-link to="/common/forgot-password" class="text-blue-600 hover:underline transition-colors">
            {{ $t('user.forgotPassword') }}
          </router-link>
          <router-link to="/common/register" class="font-medium text-blue-600 hover:underline transition-colors">
            {{ $t('user.goRegister') }}
          </router-link>
        </div>

        <!-- 登录按钮 -->
        <el-button 
          type="primary" 
          size="large" 
          class="w-full h-12 text-lg font-medium tracking-wide shadow-lg shadow-blue-500/20 hover:shadow-blue-500/30 transition-all" 
          :loading="loading"
          @click="handleLogin"
        >
          {{ $t('user.loginButton') }}
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
