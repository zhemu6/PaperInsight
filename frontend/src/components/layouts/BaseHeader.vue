<script lang="ts" setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { toggleDark } from '~/composables'
import { useUserStore } from '~/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled, SwitchButton, User } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const loginUser = computed(() => userStore.loginUser)

const handleCommand = (command: string) => {
  if (command === 'profile') {
    router.push('/user/profile')
  } else if (command === 'logout') {
    handleLogout()
  }
}

const handleLogout = async () => {
    try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })
        await userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/common/login')
    } catch (e) {
        // cancelled or failed
    }
}
</script>

<template>
  <el-menu class="el-menu-demo flex items-center px-4" mode="horizontal" :ellipsis="false">
    <!-- Logo Area -->
    <div class="flex items-center gap-2 mr-auto cursor-pointer" @click="router.push('/')">
        <div class="i-ep-element-plus text-2xl text-[var(--el-color-primary)]"></div>
        <span class="font-bold text-lg hidden sm:block">PaperInsight</span>
    </div>

    <!-- Theme Toggle -->
    <button
      class="border-none bg-transparent cursor-pointer flex items-center justify-center p-2 mr-4 hover:bg-[var(--el-fill-color)] rounded-full transition-colors"
      @click="toggleDark()"
      title="切换主题"
    >
      <i class="inline-flex text-xl i-ep-sunny dark:i-ep-moon" />
    </button>

    <!-- User Profile Dropdown -->
    <el-dropdown @command="handleCommand" trigger="click">
      <div class="flex items-center gap-2 cursor-pointer outline-none hover:opacity-80 transition-opacity">
        <el-avatar 
            :size="32" 
            :src="loginUser.userAvatar" 
            :icon="UserFilled"
            class="border border-gray-200 dark:border-gray-700"
        />
        <span class="max-w-[100px] truncate text-sm font-medium">{{ loginUser.userName || '用户' }}</span>
        <div class="i-ep-arrow-down text-xs text-gray-400"></div>
      </div>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="profile">
             <el-icon><User /></el-icon>个人中心
          </el-dropdown-item>
          <el-dropdown-item divided command="logout">
             <el-icon><SwitchButton /></el-icon>退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </el-menu>
</template>

<style lang="scss" scoped>
.el-menu-demo {
    border-bottom: none;
    height: 100%;
}
</style>
