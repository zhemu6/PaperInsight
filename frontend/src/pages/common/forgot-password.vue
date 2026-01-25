<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { resetPassword } from '~/api/sysUserController'
import bgImage from '~/assets/login_bg.png' // Shared background
import logo from '~/assets/vue.svg'
import request from '~/request'
import { useDark } from '@vueuse/core'

const isDark = useDark()
isDark.value = false // 强制关闭暗黑模式

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
    ElMessage.warning('请输入邮箱地址')
    return
  }
  if (!/^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/.test(form.email)) {
    ElMessage.warning('请输入有效的邮箱地址')
    return
  }
  
  try {
    codeLoading.value = true
    await request.post('/user/code/send', null, {
      params: { email: form.email }
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

const handleReset = async () => {
  if (!form.email || !form.code || !form.newPassword || !form.checkPassword) {
    ElMessage.warning('请填写所有必填项')
    return
  }
  if (form.newPassword !== form.checkPassword) {
    ElMessage.warning('两次输入的密码不一致')
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
      ElMessage.success('密码重置成功，请重新登录')
      ElMessage.success('密码重置成功，请重新登录')
      router.push('/common/login')
    } else {
      ElMessage.error(res.message || '重置失败')
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
        <div class="w-10 h-10 bg-white/20 backdrop-blur-md rounded-lg flex items-center justify-center border border-white/30">
          <img :src="logo" alt="Logo" class="w-6 h-6" />
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
            智能工具
          </div>
          <div class="px-4 py-2 rounded-full bg-white/10 backdrop-blur-md border border-white/20 flex items-center gap-2 text-sm font-light">
            <span class="w-1.5 h-1.5 rounded-full bg-blue-400"></span>
            高效协作
          </div>
        </div>
      </div>

      <!-- 底部引用 -->
      <div class="absolute bottom-10 left-12 text-white/60 text-xs font-serif italic tracking-wider z-10">
        " 创新始于洞察 "
      </div>
    </div>

    <!-- 右侧：重置密码表单 -->
    <div class="w-full md:w-1/2 flex items-center justify-center p-8 md:p-16 relative">
      <div class="w-full max-w-[420px]">
        <!-- 标题 -->
        <div class="mb-10">
          <h1 class="text-3xl font-serif text-gray-900 mb-3 tracking-wide">重置密码</h1>
          <p class="text-gray-500 text-sm">请输入您的邮箱以重置密码</p>
        </div>

        <!-- 表单 -->
        <div class="space-y-5">
          <el-input 
            v-model="form.email" 
            placeholder="请输入邮箱地址" 
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
              placeholder="验证码" 
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
              {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
            </el-button>
          </div>
          <el-input 
            v-model="form.newPassword" 
            placeholder="请输入新密码" 
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
            placeholder="请确认新密码" 
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
          <router-link to="/common/login" class="text-gray-500 hover:text-gray-900 flex items-center gap-1 transition-colors">
            <div class="i-ep-arrow-left" />
            返回登录
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
          确认重置
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
