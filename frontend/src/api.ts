import axios, { AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult, ApiError, ErrorCode } from './types'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Token 管理函数
export function setAuthToken(token: string | null) {
  if (token) {
    api.defaults.headers.common['Authorization'] = `Bearer ${token}`
  } else {
    delete api.defaults.headers.common['Authorization']
  }
}

// 获取当前存储的 token
function getStoredToken(): string | null {
  return localStorage.getItem('accessToken')
}

// 清除认证状态并跳转登录
function clearAuthState() {
  localStorage.removeItem('accessToken')
  setAuthToken(null)
}

// 是否正在处理401的标记，防止多次触发
let isHandling401 = false

/**
 * 处理401未授权错误
 * 清除认证状态并跳转到登录页，保存原路径以便登录后返回
 */
function handle401Unauthorized() {
  if (isHandling401) return
  isHandling401 = true

  // 保存当前路径，登录后可以返回
  const currentPath = window.location.pathname
  if (currentPath !== '/login' && currentPath !== '/') {
    sessionStorage.setItem('redirectAfterLogin', currentPath)
  }

  // 清除认证状态
  clearAuthState()

  // 显示提示消息
  ElMessage.warning('登录已过期，请重新登录')

  // 延迟跳转，让用户看到提示
  setTimeout(() => {
    window.location.href = '/login'
    isHandling401 = false
  }, 800)
}

/**
 * 处理403权限不足错误
 * 显示错误提示，不跳转页面
 */
function handle403Forbidden(message: string) {
  ElMessage.error(message || '抱歉，您没有权限执行此操作')
}

// 错误消息映射（根据接口文档 2.5）
const ERROR_MESSAGES: Record<number, string> = {
  40001: '请求参数错误',
  40101: '未登录或登录已过期，请重新登录',
  40301: '无操作权限',
  40401: '请求的资源不存在',
  40901: '数据状态冲突，请刷新后重试',
  41301: '上传文件过大',
  42901: '请求过于频繁，请稍后再试',
  50001: '系统内部错误，请稍后重试',
  50201: '模型服务暂时不可用',
  50202: '知识库服务暂时不可用',
  50203: '天气服务暂时不可用',
  50401: '外部服务响应超时'
}

// 获取友好的错误消息
function getErrorMessage(error: AxiosError<ApiError>): string {
  // 优先使用后端返回的具体错误消息
  if (error.response?.data?.message) {
    return error.response.data.message
  }

  // 根据业务错误码返回预设消息
  const errorCode = error.response?.data?.code
  if (errorCode && ERROR_MESSAGES[errorCode]) {
    return ERROR_MESSAGES[errorCode]
  }

  // 根据 HTTP 状态码返回通用消息
  const status = error.response?.status
  if (status === 401) {
    return '未登录或登录已过期，请重新登录'
  }
  if (status === 403) {
    return '无操作权限'
  }
  if (status === 404) {
    return '请求的资源不存在'
  }
  if (status === 429) {
    return '请求过于频繁，请稍后再试'
  }
  if (status && status >= 500) {
    return '服务器暂时不可用，请稍后重试'
  }
  if (error.code === 'ECONNABORTED') {
    return '请求超时，请检查网络后重试'
  }
  if (!error.response) {
    return '网络连接失败，请检查网络'
  }

  return '服务暂时不可用，请稍后重试'
}

// 请求拦截器
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getStoredToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    // 成功响应直接返回
    return response
  },
  (error: AxiosError<ApiError>) => {
    const status = error.response?.status
    const errorCode = error.response?.data?.code
    const message = getErrorMessage(error)

    // 401 未授权：清除认证状态并跳转到登录页
    if (status === 401 || errorCode === 40101) {
      handle401Unauthorized()
    }
    // 403 权限不足：显示错误提示
    else if (status === 403 || errorCode === 40301) {
      handle403Forbidden(message)
    }
    // 429 请求过于频繁
    else if (status === 429 || errorCode === 42901) {
      ElMessage.warning(message)
    }
    // 5xx 服务器错误
    else if (status && status >= 500) {
      ElMessage.error(message)
    }
    // 其他错误（网络、超时等）
    else {
      ElMessage.error(message)
    }

    return Promise.reject(error)
  }
)

// 解包 API 响应数据
// 兼容统一响应格式 { code, message, data } 和直接返回的数据
export const unwrap = <T>(result: { data: any }): T => {
  const body = result.data
  // 统一响应格式：有 code 和 data 字段时取 data
  if (body && typeof body === 'object' && 'code' in body && 'data' in body) {
    return body.data as T
  }
  // 后端直接返回数据对象（未包裹在统一响应格式中）
  return body as T
}

export default api
