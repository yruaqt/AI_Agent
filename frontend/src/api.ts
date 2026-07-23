import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from './types'

const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE || '/api/v1', timeout: 30000 })
api.interceptors.request.use(config => { const token=localStorage.getItem('accessToken'); if(token) config.headers.Authorization=`Bearer ${token}`; return config })
api.interceptors.response.use(response => response, error => {
  const status=error.response?.status; const message=error.response?.data?.message || '服务暂时不可用'
  if(status===401){ localStorage.removeItem('accessToken'); if(location.pathname!=='/login') location.href='/login' }
  else ElMessage.error(message)
  return Promise.reject(error)
})
export const unwrap = <T>(result:{data:ApiResult<T>}) => result.data.data
export default api

