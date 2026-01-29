import type { AxiosRequestConfig } from 'axios'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { i18n } from '~/i18n'

// 创建 Axios 实例
const myAxios = axios.create({
  baseURL: '/api',
  timeout: 60000,
  withCredentials: true,
})

// 全局请求拦截器
myAxios.interceptors.request.use(
  (config) => {
    // Do something before request is sent
    return config
  },
  (error) => {
    // Do something with request error
    return Promise.reject(error)
  },
)

// 全局响应拦截器
myAxios.interceptors.response.use(
  (response) => {
    const { data } = response

    // 未登录
    if (data.code === 40100) {
      if (
        !response.request.responseURL.includes('user/get/login')
        && !window.location.pathname.includes('/common/login')
      ) {
        ElMessage.warning(i18n.global.t('error.unauthorized'))
        // 只保存路径和查询参数，避免完整 URL 过长
        const redirectPath = window.location.pathname + window.location.search
        window.location.href = `/common/login?redirect=${encodeURIComponent(redirectPath)}`
      }
      return Promise.reject(data)
    }

    // 其他业务错误 (非 0)
    if (data.code !== 0) {
      ElMessage.error(data.message || i18n.global.t('error.systemError'))
      return Promise.reject(data)
    }

    // 成功
    return data
  },
  (error) => {
    // Any status codes that falls outside the range of 2xx cause this function to trigger
    // Do something with response error
    return Promise.reject(error)
  },
)

// Add a type declaration to override the return type
declare module 'axios' {
  interface AxiosInstance {
    <T = any>(config: AxiosRequestConfig): Promise<T>
    <T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
    request: <T = any>(config: AxiosRequestConfig) => Promise<T>
    get: <T = any>(url: string, config?: AxiosRequestConfig) => Promise<T>
    delete: <T = any>(url: string, config?: AxiosRequestConfig) => Promise<T>
    head: <T = any>(url: string, config?: AxiosRequestConfig) => Promise<T>
    post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) => Promise<T>
    put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) => Promise<T>
    patch: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) => Promise<T>
  }
}

export default myAxios
