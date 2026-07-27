<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import api, { unwrap } from '@/api'
import type { Orchard, PageData } from '@/types'
import {
  DataAnalysis,
  ChatDotRound,
  Calendar,
  SetUp,
  Notebook,
  Collection,
  Files,
  User,
  Fold,
  Expand,
  SwitchButton,
  CaretBottom,
  Location,
  Orange,
  Bell,
  Search,
  WarningFilled
} from '@element-plus/icons-vue'
import { ElDropdown, ElMessage } from 'element-plus'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const collapsed = ref(false)
const mobileOpen = ref(false)
const orchardList = ref<Orchard[]>([])
const currentOrchard = ref<Orchard | null>(null)
const orchardLoading = ref(false)

// 全局搜索
const searchDialog = ref(false)
const searchQuery = ref('')
const searchResults = ref<any[]>([])
const searching = ref(false)

// 消息通知
const notificationVisible = ref(false)
const notifications = ref<any[]>([])

const menus = [
  { path: '/', label: '果园总览', icon: DataAnalysis },
  { path: '/chat', label: '智能问答', icon: ChatDotRound },
  { path: '/tasks', label: '今日农事', icon: Calendar },
  { path: '/calculators', label: '用量计算', icon: SetUp },
  { path: '/records', label: '实训记录', icon: Notebook },
  { path: '/orchards', label: '果园档案', icon: Collection },
  { path: '/knowledge', label: '知识库', icon: Files, admin: true },
  { path: '/users', label: '用户管理', icon: User, admin: true }
]

const visibleMenus = computed(() =>
  menus.filter(x => !x.admin || auth.isAdmin)
)

const pageTitle = computed(() =>
  menus.find(x => x.path === route.path)?.label || '榄园知行'
)

const selectedOrchardId = computed({
  get: () => currentOrchard.value?.id || '',
  set: (id: string) => {
    const o = orchardList.value.find(x => x.id === id)
    if (o) selectOrchard(o)
  }
})

const phenologyNames: Record<string, string> = {
  DORMANCY: '休眠/恢复期',
  SHOOT_GROWTH: '春梢生长期',
  FLOWERING: '开花期',
  FRUIT_SET: '坐果期',
  FRUIT_EXPANSION: '幼果膨大期',
  MATURITY: '成熟期',
  HARVEST: '采收期',
  POST_HARVEST: '采后管理期'
}

async function loadOrchards() {
  orchardLoading.value = true
  try {
    const data = unwrap<PageData<Orchard>>(
      await api.get('/orchards', { params: { pageSize: 50, status: 'ENABLED' } })
    )
    orchardList.value = data.items
    if (data.items.length > 0 && !currentOrchard.value) {
      currentOrchard.value = data.items[0]
      localStorage.setItem('currentOrchardId', data.items[0].id)
    } else if (currentOrchard.value) {
      const saved = data.items.find(o => o.id === currentOrchard.value?.id)
      if (saved) currentOrchard.value = saved
    }
  } catch {
    orchardList.value = []
  } finally {
    orchardLoading.value = false
  }
}

function selectOrchard(orchard: Orchard) {
  currentOrchard.value = orchard
  localStorage.setItem('currentOrchardId', orchard.id)
}

function go(path: string) {
  router.push(path)
  mobileOpen.value = false
}

function logout() {
  auth.logout()
  router.push('/login')
}

const userDropdownItems = [
  { label: '个人设置', icon: 'User', disabled: true },
  { label: '退出登录', icon: 'SwitchButton', divided: true, command: 'logout' }
]

function handleUserCommand(command: string) {
  if (command === 'logout') logout()
}

function openSearchDialog() {
  searchDialog.value = true
  searchQuery.value = ''
  searchResults.value = []
}

async function runGlobalSearch() {
  if (!searchQuery.value.trim()) {
    ElMessage.warning('请输入检索内容')
    return
  }
  searching.value = true
  try {
    const payload = {
      query: searchQuery.value.trim(),
      maxResults: 5,
      minScore: 0.65
    }
    const data = unwrap<any[]>(await api.post('/knowledge/search-test', payload))
    searchResults.value = data || []
  } catch (e: any) {
    ElMessage.error(e?.message || '检索失败')
    searchResults.value = []
  } finally {
    searching.value = false
  }
}

onMounted(() => {
  const savedId = localStorage.getItem('currentOrchardId')
  if (savedId) {
    currentOrchard.value = { id: savedId } as Orchard
  }
  loadOrchards()
})

watch(
  () => route.path,
  () => {
    mobileOpen.value = false
  }
)
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{ collapsed }">
      <button class="brand" title="返回果园总览" @click="go('/')">
        <span class="brand-mark">榄</span>
        <span v-if="!collapsed" class="brand-copy">
          <strong>榄园知行</strong>
          <small>OLIVE ORCHARD OS</small>
        </span>
      </button>

      <div v-if="!collapsed && orchardLoading" class="orchard-switcher">
        <div class="orchard-select skeleton-orchard">
          <div class="skeleton-avatar-sm"></div>
          <div class="orchard-info">
            <div class="skeleton-line skeleton-line--title"></div>
            <div class="skeleton-line skeleton-line--tiny"></div>
          </div>
        </div>
      </div>
      <div v-else-if="!collapsed && orchardList.length > 0" class="orchard-switcher">
        <el-dropdown trigger="click" @command="(id: string) => {
          const o = orchardList.find(x => x.id === id)
          if (o) selectOrchard(o)
        }">
          <div class="orchard-select">
            <el-icon class="orchard-icon"><Orange /></el-icon>
            <div class="orchard-info">
              <span class="orchard-name">{{ currentOrchard?.name || '选择果园' }}</span>
              <span class="orchard-phenology">
                {{ phenologyNames[currentOrchard?.currentPhenology || ''] || '—' }}
              </span>
            </div>
            <el-icon class="caret"><CaretBottom /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="o in orchardList"
                :key="o.id"
                :command="o.id"
                :class="{ active: currentOrchard?.id === o.id }"
              >
                <div class="orchard-dropdown-item">
                  <span class="item-name">{{ o.name }}</span>
                  <span class="item-phenology">
                    {{ phenologyNames[o.currentPhenology] || o.currentPhenology }}
                  </span>
                </div>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <nav class="nav-menu">
        <button
          v-for="item in visibleMenus"
          :key="item.path"
          class="nav-item"
          :class="{ active: route.path === item.path }"
          :title="item.label"
          @click="go(item.path)"
        >
          <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
          <span v-if="!collapsed" class="nav-label">{{ item.label }}</span>
        </button>
      </nav>

      <button
        class="collapse-button"
        :title="collapsed ? '展开导航' : '收起导航'"
        @click="collapsed = !collapsed"
      >
        <el-icon><Expand v-if="collapsed" /><Fold v-else /></el-icon>
        <span v-if="!collapsed">收起导航</span>
      </button>
    </aside>

    <el-drawer
      v-model="mobileOpen"
      direction="ltr"
      size="280px"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="mobile-menu">
        <div class="mobile-brand">
          <span class="brand-mark">榄</span>
          <span>榄园知行</span>
        </div>

        <div v-if="orchardLoading" class="mobile-orchard">
          <div class="skeleton-select"></div>
        </div>
        <div v-else-if="orchardList.length > 0" class="mobile-orchard">
          <el-select
            v-model="selectedOrchardId"
            placeholder="选择果园"
            size="default"
            class="mobile-orchard-select"
          >
            <el-option
              v-for="o in orchardList"
              :key="o.id"
              :label="o.name"
              :value="o.id"
            />
          </el-select>
        </div>

        <div class="mobile-nav">
          <button
            v-for="item in visibleMenus"
            :key="item.path"
            class="nav-item"
            :class="{ active: route.path === item.path }"
            @click="go(item.path)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </button>
        </div>

        <div class="mobile-user">
          <span class="role-dot"></span>
          <div class="mobile-user-info">
            <strong>{{ auth.user?.displayName || '正在载入' }}</strong>
            <small>{{ auth.user?.role === 'ADMIN' ? '教师 / 管理员' : '学生' }}</small>
          </div>
          <button class="icon-button" title="退出登录" @click="logout">
            <el-icon><SwitchButton /></el-icon>
          </button>
        </div>
      </div>
    </el-drawer>

    <main class="main-area">
      <header class="topbar">
        <div class="topbar-left">
          <button
            class="icon-button mobile-only"
            title="打开导航"
            @click="mobileOpen = true"
          >
            <el-icon><Expand /></el-icon>
          </button>

          <div class="page-header">
            <div class="breadcrumb">
              <span class="breadcrumb-item">
                <el-icon><Location /></el-icon>
                {{ currentOrchard?.name || '果园数据载入中' }}
              </span>
              <span class="breadcrumb-sep">/</span>
              <span class="breadcrumb-item current">{{ pageTitle }}</span>
            </div>
            <h1 class="page-title">{{ pageTitle }}</h1>
          </div>
        </div>

        <div class="topbar-right">
          <div class="topbar-actions desktop-only">
            <button class="icon-button" title="搜索" @click="openSearchDialog">
              <el-icon><Search /></el-icon>
            </button>
            <div class="notification-wrap">
              <button
                class="icon-button notification-btn"
                title="消息通知"
                @click="notificationVisible = !notificationVisible"
              >
                <el-icon><Bell /></el-icon>
                <span v-if="notifications.length" class="notification-dot"></span>
              </button>
              <div v-show="notificationVisible" class="notification-dropdown">
                <div v-if="!notifications.length" class="notification-empty">
                  <el-icon :size="32" class="empty-icon"><WarningFilled /></el-icon>
                  <p>暂无消息通知</p>
                </div>
              </div>
            </div>
          </div>

          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="account-dropdown">
              <div class="account">
                <span class="role-dot" :class="{ admin: auth.isAdmin }"></span>
                <div class="account-info">
                  <strong>{{ auth.user?.displayName || '正在载入' }}</strong>
                  <small>{{ auth.user?.role === 'ADMIN' ? '教师 / 管理员' : '学生' }}</small>
                </div>
                <el-icon class="caret-icon"><CaretBottom /></el-icon>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <el-icon><User /></el-icon>
                  个人设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <section class="page-content">
        <router-view :key="route.fullPath" />
      </section>
    </main>

    <!-- 全局搜索弹窗 -->
    <el-dialog
      v-model="searchDialog"
      title="知识库检索"
      width="min(640px, 92vw)"
      destroy-on-close
    >
      <el-input
        v-model="searchQuery"
        placeholder="输入问题检索知识库..."
        clearable
        @keyup.enter="runGlobalSearch"
      >
        <template #append>
          <el-button :icon="Search" :loading="searching" @click="runGlobalSearch">
            检索
          </el-button>
        </template>
      </el-input>

      <div class="global-search-results">
        <div v-if="!searchResults.length && !searching && searchQuery" class="search-empty">
          未找到相关知识来源
        </div>
        <div v-if="searching" class="search-empty">正在检索...</div>

        <div
          v-for="(r, idx) in searchResults"
          :key="r.documentId + '-' + (r.chunkId || r.chunkNo || idx)"
          class="search-hit"
        >
          <header>
            <strong>{{ r.documentName || '未知来源' }}</strong>
            <span v-if="r.score !== undefined" class="hit-score">
              {{ (r.score * 100).toFixed(1) }}%
            </span>
          </header>
          <p>{{ r.content }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.skeleton-orchard {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  opacity: 0.7;
}

.skeleton-avatar-sm {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(
    90deg,
    rgba(46, 107, 78, 0.15) 25%,
    rgba(46, 107, 78, 0.08) 50%,
    rgba(46, 107, 78, 0.15) 75%
  );
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
}

.skeleton-line {
  background: linear-gradient(
    90deg,
    rgba(46, 107, 78, 0.15) 25%,
    rgba(46, 107, 78, 0.08) 50%,
    rgba(46, 107, 78, 0.15) 75%
  );
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
  border-radius: 4px;
}

.skeleton-line--title {
  width: 100px;
  height: 12px;
  margin-bottom: 6px;
}

.skeleton-line--tiny {
  width: 60px;
  height: 10px;
}

.skeleton-select {
  width: 100%;
  height: 36px;
  border-radius: 6px;
  background: linear-gradient(
    90deg,
    rgba(46, 107, 78, 0.15) 25%,
    rgba(46, 107, 78, 0.08) 50%,
    rgba(46, 107, 78, 0.15) 75%
  );
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
}

@keyframes skeleton-shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* 通知下拉面板 */
.notification-wrap {
  position: relative;
}

.notification-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 280px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(46, 107, 78, 0.12);
  z-index: 100;
  padding: 16px;
}

.notification-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #888;
  padding: 24px 0;
}

.notification-empty .empty-icon {
  color: #c0c4cc;
}

/* 全局搜索弹窗结果 */
.global-search-results {
  margin-top: 16px;
  max-height: 400px;
  overflow-y: auto;
}

.search-empty {
  text-align: center;
  color: #888;
  padding: 24px 0;
}

.search-hit {
  border: 1px solid rgba(46, 107, 78, 0.12);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
  background: #f8faf9;
}

.search-hit header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.search-hit strong {
  color: #2e6b4e;
  font-size: 14px;
}

.hit-score {
  font-size: 12px;
  color: #fff;
  background: #2e6b4e;
  padding: 2px 8px;
  border-radius: 12px;
}

.search-hit p {
  margin: 0;
  font-size: 13px;
  color: #555;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
