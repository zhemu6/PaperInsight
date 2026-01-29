<script lang="ts" setup>
import { SwitchButton, User, UserFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import logo from '~/assets/logo.svg'
import { toggleDark } from '~/composables'
import { getLocale, toggleLocale } from '~/i18n'
import { useUserStore } from '~/stores/user'

const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()

const loginUser = computed(() => userStore.loginUser)
const currentLocale = computed(() => getLocale())

function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/user/profile')
  }
  else if (command === 'logout') {
    handleLogout()
  }
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm(t('user.logoutConfirm') || '确定要退出登录吗？', t('common.warning'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
    await userStore.logout()
    ElMessage.success(t('user.logoutSuccess'))
    router.push('/common/login')
  }
  catch (e) {
    // cancelled or failed
  }
}

// 切换语言
function handleToggleLocale() {
  const newLocale = toggleLocale()
  ElMessage.success(newLocale === 'zh' ? '已切换到中文' : 'Switched to English')
}
</script>

<template>
  <el-menu class="el-menu-demo flex items-center px-4" mode="horizontal" :ellipsis="false">
    <!-- Logo Area -->
    <div class="mr-auto flex cursor-pointer items-center gap-2" @click="router.push('/')">
      <img :src="logo" alt="PaperInsight Logo" class="h-8 w-8">
      <span class="hidden text-lg font-bold sm:block">PaperInsight</span>
    </div>

    <!-- Language Toggle -->
    <button
      class="mr-2 flex cursor-pointer items-center justify-center rounded-full border-none bg-transparent p-2 transition-colors hover:bg-[var(--el-fill-color)]"
      :title="$t('common.language')"
      @click="handleToggleLocale"
    >
      <span class="text-sm font-medium">{{ currentLocale === 'zh' ? '中' : 'EN' }}</span>
    </button>

    <!-- Theme Toggle -->
    <button
      class="mr-4 flex cursor-pointer items-center justify-center rounded-full border-none bg-transparent p-2 transition-colors hover:bg-[var(--el-fill-color)]"
      :title="$t('common.language') === 'Language' ? 'Toggle Theme' : '切换主题'"
      @click="toggleDark()"
    >
      <i class="i-ep-sunny dark:i-ep-moon inline-flex text-xl" />
    </button>

    <!-- User Profile Dropdown -->
    <el-dropdown trigger="click" @command="handleCommand">
      <div class="flex cursor-pointer items-center gap-2 outline-none transition-opacity hover:opacity-80">
        <el-avatar
          :size="32"
          :src="loginUser.userAvatar"
          :icon="UserFilled"
          class="border border-gray-200 dark:border-gray-700"
        />
        <span class="max-w-[100px] truncate text-sm font-medium">{{ loginUser.userName || $t('user.notLoggedIn') }}</span>
        <div class="i-ep-arrow-down text-xs text-gray-400" />
      </div>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="profile">
            <el-icon><User /></el-icon>{{ $t('nav.profile') }}
          </el-dropdown-item>
          <el-dropdown-item divided command="logout">
            <el-icon><SwitchButton /></el-icon>{{ $t('nav.logout') }}
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
