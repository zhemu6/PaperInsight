import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 Axios 实例
const myAxios = axios.create({
  baseURL: '/api',
  timeout: 60000,
  withCredentials: true,
})

// 全局请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    // Do something before request is sent
    return config
  },
  function (error) {
    // Do something with request error
    return Promise.reject(error)
  },
)

// 全局响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    const { data } = response

    // 未登录
    if (data.code === 40100) {
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        ElMessage.warning('请先登录')
        window.location.href = `/user/login?redirect=${window.location.href}`
      }
      return Promise.reject(data)
    }

    // 其他业务错误 (非 0)
    if (data.code !== 0) {
      ElMessage.error(data.message || '系统错误')
      return Promise.reject(data)
    }

    // 成功
    return data
  },
  function (error) {
    // Any status codes that falls outside the range of 2xx cause this function to trigger
    // Do something with response error
    return Promise.reject(error)
  },
)

export default myAxios
