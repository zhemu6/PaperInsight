import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getLoginUser, userLogout } from '~/api/sysUserController'

export const useUserStore = defineStore(
  'user',
  () => {
    const loginUser = ref<API.SysUserVO>({
      userName: '未登录',
    })

    async function fetchLoginUser() {
      try {
        const res = await getLoginUser()
        if (res.code === 0 && res.data) {
          loginUser.value = res.data
        }
      } catch (error) {
        // 获取失败，可能是没登录，保持默认
      }
    }

    function setLoginUser(user: API.SysUserVO) {
      loginUser.value = user
    }

    async function logout() {
      await userLogout()
      loginUser.value = { userName: '未登录' }
    }

    return {
      loginUser,
      fetchLoginUser,
      setLoginUser,
      logout,
    }
  },
  {
    persist: true, // 开启持久化
  },
)
