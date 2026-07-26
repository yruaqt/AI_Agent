import { defineStore } from 'pinia'
import api, { unwrap, setAuthToken } from '@/api'
import type { User, LoginResult } from '@/types'

const TOKEN_KEY = 'accessToken'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null as User | null,
    loading: false,
    initialized: false
  }),
  getters: {
    isLoggedIn: (state) => !!state.user,
    isAdmin: (state) => state.user?.role === 'ADMIN',
    isStudent: (state) => state.user?.role === 'STUDENT'
  },
  actions: {
    async login(username: string, password: string) {
      this.loading = true
      try {
        const data = unwrap<LoginResult>(
          await api.post('/auth/login', { username, password })
        )
        const token = data.accessToken
        localStorage.setItem(TOKEN_KEY, token)
        setAuthToken(token)
        this.user = data.user
        this.initialized = true
        return data
      } finally {
        this.loading = false
      }
    },
    async load() {
      if (this.loading || this.initialized) return
      const token = localStorage.getItem(TOKEN_KEY)
      if (!token) {
        this.initialized = true
        return
      }
      this.loading = true
      setAuthToken(token)
      try {
        this.user = unwrap<User>(await api.get('/auth/me'))
      } catch (e: any) {
        if (e.response?.status === 401) {
          this.clearAuth()
        }
        throw e
      } finally {
        this.loading = false
        this.initialized = true
      }
    },
    logout() {
      // 首版采用无状态退出；后端保留 /auth/logout 接口以便后续扩展黑名单
      this.clearAuth()
    },
    clearAuth() {
      localStorage.removeItem(TOKEN_KEY)
      setAuthToken(null)
      this.user = null
      this.initialized = true
    }
  }
})
