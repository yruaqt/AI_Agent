import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LoginView from '@/views/LoginView.vue'
import ForbiddenView from '@/views/ForbiddenView.vue'
import NotFoundView from '@/views/NotFoundView.vue'
import AppShell from '@/components/AppShell.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    { path: '/403', name: 'forbidden', component: ForbiddenView },
    {
      path: '/',
      component: AppShell,
      children: [
        { path: '', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
        { path: 'chat', name: 'chat', component: () => import('@/views/ChatView.vue') },
        { path: 'tasks', name: 'tasks', component: () => import('@/views/TasksView.vue') },
        { path: 'calculators', name: 'calculators', component: () => import('@/views/CalculatorsView.vue') },
        { path: 'records', name: 'records', component: () => import('@/views/RecordsView.vue') },
        { path: 'orchards', name: 'orchards', component: () => import('@/views/OrchardsView.vue') },
        { path: 'knowledge', name: 'knowledge', component: () => import('@/views/KnowledgeView.vue'), meta: { admin: true } },
        { path: 'users', name: 'users', component: () => import('@/views/UsersView.vue'), meta: { admin: true } }
      ]
    },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView }
  ]
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (!auth.initialized) {
    try {
      await auth.load()
    } catch {
      // 加载失败（如 401）会在 store 内清除状态，继续后续导航逻辑
    }
  }

  const isLoggedIn = auth.isLoggedIn

  if (!isLoggedIn && to.path !== '/login') {
    return '/login'
  }
  if (isLoggedIn && to.path === '/login') {
    return '/'
  }
  // 管理员权限检查失败时跳转到403页面
  if (to.meta.admin && !auth.isAdmin) {
    return '/403'
  }
})

export default router
