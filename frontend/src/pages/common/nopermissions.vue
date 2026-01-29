<script setup lang="ts">
import { Key, Lock, Message, User } from '@element-plus/icons-vue'
import { useDark } from '@vueuse/core'
import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { userLogin } from '~/api/sysUserController'
import bgImage from '~/assets/login_bg.png'
import logo from '~/assets/logo.svg'
import request from '~/request'

const isDark = useDark()
isDark.value = false // 强制关闭暗黑模式

const router = useRouter()
const activeTab = ref('account')

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
    ElMessage.warning('请输入邮箱地址')
    return
  }
  if (!/^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/.test(emailForm.email)) {
    ElMessage.warning('请输入有效的邮箱地址')
    return
  }

  try {
    codeLoading.value = true
    await request.post('/user/code/send', null, {
      params: { email: emailForm.email },
    })

    ElMessage.success('验证码已发送')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  }
  catch (error: any) {
    ElMessage.error(error.message || '发送验证码失败')
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
        ElMessage.warning('请输入账号和密码')
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
        ElMessage.warning('请输入邮箱和验证码')
        return
      }
      res = await userLogin({
        email: emailForm.email,
        code: emailForm.code,
        type: 'email',
      })
    }

    if (res.code === 0 && res.data) {
      ElMessage.success('登录成功')
      router.push('/')
    }
    else {
      ElMessage.error(res.message || '登录失败')
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

<template>
  <div class="min-h-screen w-full flex bg-white">
    <!-- 左侧：背景图 -->
    <div class="relative hidden w-1/2 bg-cover bg-center md:flex" :style="{ backgroundImage: `url(${bgImage})` }">
      <!-- 遮罩层 -->
      <div class="absolute inset-0 bg-blue-900/30 backdrop-blur-[2px]" />

      <!-- 左上角：Logo -->
      <div class="absolute left-8 top-8 z-10 flex items-center gap-4 text-white">
        <div class="h-12 w-12 flex items-center justify-center border border-white/30 rounded-lg bg-white/20 backdrop-blur-md">
          <img :src="logo" alt="Logo" class="h-8 w-8">
        </div>
        <span class="text-2xl font-bold tracking-wide">PaperInsight</span>
      </div>

      <!-- 中间内容 -->
      <div class="absolute left-0 right-0 top-1/2 z-10 transform px-12 text-white -translate-y-1/2">
        <h2 class="mb-6 text-5xl font-bold leading-tight tracking-wide font-serif">
          挖掘学术论文的<br>无限潜力
        </h2>
        <div class="mb-6 h-1 w-12 bg-white/50" />
        <p class="mb-10 text-xl text-white/90 font-light tracking-widest">
          智能研读 · 深度洞察 · 高效科研
        </p>

        <!-- 特性标签 -->
        <div class="flex gap-4">
          <div class="flex items-center gap-2 border border-white/20 rounded-full bg-white/10 px-4 py-2 text-sm font-light backdrop-blur-md">
            <span class="h-1.5 w-1.5 rounded-full bg-emerald-400" />
            AI 智能分析
          </div>
          <div class="flex items-center gap-2 border border-white/20 rounded-full bg-white/10 px-4 py-2 text-sm font-light backdrop-blur-md">
            <span class="h-1.5 w-1.5 rounded-full bg-blue-400" />
            精准文献解析
          </div>
        </div>
      </div>

      <!-- 底部引用 -->
      <div class="absolute bottom-10 left-12 z-10 text-xs text-white/60 tracking-wider font-serif italic">
        " 让科研更智能，而非更繁琐 "
      </div>
    </div>

    <!-- 右侧：登录表单 -->
    <div class="relative w-full flex items-center justify-center p-8 md:w-1/2 md:p-16">
      <div class="max-w-[420px] w-full">
        <!-- 标题 -->
        <div class="mb-10">
          <h1 class="mb-3 text-3xl text-gray-900 tracking-wide font-serif">
            欢迎回来
          </h1>
          <p class="text-sm text-gray-500">
            登录以继续使用 PaperInsight
          </p>
        </div>

        <!-- 切换 Tab -->
        <!-- 切换 Tab (胶囊样式，无黑边) -->
        <div class="grid grid-cols-2 mb-10 gap-1 rounded-xl bg-gray-50 p-1">
          <button
            class="rounded-lg py-2.5 text-sm font-medium transition-all duration-200"
            :class="activeTab === 'account' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'"
            @click="activeTab = 'account'"
          >
            账号登录
          </button>
          <button
            class="rounded-lg py-2.5 text-sm font-medium transition-all duration-200"
            :class="activeTab === 'email' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'"
            @click="activeTab = 'email'"
          >
            邮箱登录
          </button>
        </div>

        <!-- 账号表单 -->
        <div v-if="activeTab === 'account'" class="space-y-6">
          <el-input
            v-model="accountForm.userAccount"
            placeholder="请输入账号"
            :prefix-icon="User"
            size="large"
            class="custom-input h-12"
          />
          <el-input
            v-model="accountForm.userPassword"
            placeholder="请输入密码"
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
            placeholder="请输入邮箱地址"
            :prefix-icon="Message"
            size="large"
            class="custom-input h-12"
          />
          <div class="flex gap-4">
            <el-input
              v-model="emailForm.code"
              placeholder="验证码"
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
              {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </div>

        <!-- 底部链接 -->
        <div class="mb-8 mt-6 flex items-center justify-between text-sm">
          <router-link to="/user/forgot-password" class="text-blue-600 transition-colors hover:underline">
            忘记密码？
          </router-link>
          <router-link to="/user/register" class="text-blue-600 font-medium transition-colors hover:underline">
            立即注册
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
          登 录
        </el-button>

        <!-- 底部版权 -->
        <div class="mt-10 text-center text-xs text-gray-400 font-light tracking-wider">
          © 2026 PaperInsight · 赋能科研新范式
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
