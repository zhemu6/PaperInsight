<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock, Message, Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { userLogin } from '~/api/sysUserController'
import { useUserStore } from '~/stores/user'
import bgImage from '~/assets/login_bg.png'
import logo from '~/assets/logo.svg'
import request from '~/request'
import { useDark } from '@vueuse/core'

const isDark = useDark()
isDark.value = false // 强制关闭暗黑模式


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
      params: { email: emailForm.email }
    })
    
    ElMessage.success('验证码已发送')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error: any) {
    ElMessage.error(error.message || '发送验证码失败')
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
        ElMessage.warning('请输入账号和密码')
        return
      }
      res = await userLogin({
        userAccount: accountForm.userAccount,
        userPassword: accountForm.userPassword,
        type: 'account'
      })
    } else {
      if (!emailForm.email || !emailForm.code) {
        ElMessage.warning('请输入邮箱和验证码')
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
      ElMessage.success('登录成功')
      // 跳转到重定向地址或首页
      const redirectPath = getRedirectPath()
      router.push(redirectPath)
    } else {
      ElMessage.error(res.message || '登录失败')
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
  <div class="min-h-screen w-full flex bg-white">
    <!-- 左侧：背景图 -->
    <div class="hidden md:flex w-1/2 relative bg-cover bg-center" :style="{ backgroundImage: `url(${bgImage})` }">
      <!-- 遮罩层 -->
      <div class="absolute inset-0 bg-blue-900/30 backdrop-blur-[2px]"></div>

      <!-- 左上角：Logo -->
      <div class="absolute top-8 left-8 flex items-center gap-4 text-white z-10">
        <div class="w-12 h-12 bg-white/20 backdrop-blur-md rounded-lg flex items-center justify-center border border-white/30">
          <img :src="logo" alt="Logo" class="w-8 h-8" />
        </div>
        <span class="text-2xl font-bold tracking-wide">PaperInsight</span>
      </div>

      <!-- 中间内容 -->
      <div class="absolute top-1/2 left-0 right-0 transform -translate-y-1/2 px-12 text-white z-10">
        <h2 class="text-5xl font-serif font-bold leading-tight mb-6 tracking-wide">
          挖掘学术论文的<br/>无限潜力
        </h2>
        <div class="w-12 h-1 bg-white/50 mb-6"></div>
        <p class="text-xl text-white/90 font-light tracking-widest mb-10">
          智能研读 · 深度洞察 · 高效科研
        </p>

        <!-- 特性标签 -->
        <div class="flex gap-4">
          <div class="px-4 py-2 rounded-full bg-white/10 backdrop-blur-md border border-white/20 flex items-center gap-2 text-sm font-light">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
            AI 智能分析
          </div>
          <div class="px-4 py-2 rounded-full bg-white/10 backdrop-blur-md border border-white/20 flex items-center gap-2 text-sm font-light">
            <span class="w-1.5 h-1.5 rounded-full bg-blue-400"></span>
            精准文献解析
          </div>
        </div>
      </div>

      <!-- 底部引用 -->
      <div class="absolute bottom-10 left-12 text-white/60 text-xs font-serif italic tracking-wider z-10">
        " 让科研更智能，而非更繁琐 "
      </div>
    </div>

    <!-- 右侧：登录表单 -->
    <div class="w-full md:w-1/2 flex items-center justify-center p-8 md:p-16 relative">
      <div class="w-full max-w-[420px]">
        <!-- 标题 -->
        <div class="mb-10">
          <h1 class="text-3xl font-serif text-gray-900 mb-3 tracking-wide">欢迎回来</h1>
          <p class="text-gray-500 text-sm">登录以继续使用 PaperInsight</p>
        </div>

        <!-- 切换 Tab -->
        <!-- 切换 Tab (胶囊样式，无黑边) -->
        <div class="grid grid-cols-2 gap-1 p-1 mb-10 bg-gray-50 rounded-xl">
          <button
            @click="activeTab='account'"
            class="py-2.5 text-sm font-medium rounded-lg transition-all duration-200"
            :class="activeTab==='account' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'"
          >
            账号登录
          </button>
          <button
            @click="activeTab='email'"
            class="py-2.5 text-sm font-medium rounded-lg transition-all duration-200"
            :class="activeTab==='email' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'"
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
              {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
            </el-button>
          </div>
        </div>

        <!-- 底部链接 -->
        <div class="flex justify-between items-center text-sm mt-6 mb-8">
          <router-link to="/common/forgot-password" class="text-blue-600 hover:underline transition-colors">
            忘记密码？
          </router-link>
          <router-link to="/common/register" class="font-medium text-blue-600 hover:underline transition-colors">
            立即注册
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
          登 录
        </el-button>

        <!-- 底部版权 -->
        <div class="text-center mt-10 text-xs text-gray-400 font-light tracking-wider">
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
