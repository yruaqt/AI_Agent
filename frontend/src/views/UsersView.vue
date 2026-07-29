<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import api, { unwrap } from '@/api'
import type { User, PageData } from '@/types'
import {
  Plus,
  Refresh,
  Search,
  Key,
  Check,
  Close,
  Delete,
  User as UserIcon
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import SkeletonTable from '@/components/SkeletonTable.vue'
import ErrorState from '@/components/ErrorState.vue'
import EmptyState from '@/components/EmptyState.vue'

const auth = useAuthStore()

const users = ref<User[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const deletingUserId = ref<string | null>(null)

const pagination = reactive({
  page: 1,
  pageSize: 15,
  total: 0
})

const search = reactive({
  keyword: '',
  status: ''
})

// 新增用户对话框
const dialog = ref(false)
const submitting = ref(false)
const form = reactive({
  username: '',
  displayName: '',
  password: '123456',
  role: 'STUDENT' as 'STUDENT' | 'ADMIN'
})

// 重置密码对话框
const resetDialog = ref(false)
const resetSubmitting = ref(false)
const resetTarget = ref<User | null>(null)
const resetForm = reactive({
  newPassword: ''
})

const roleMap: Record<string, { label: string; class: string }> = {
  ADMIN: { label: '教师 / 管理员', class: 'role-admin' },
  STUDENT: { label: '学生', class: 'role-student' }
}

const statusMap: Record<string, { label: string; class: string }> = {
  ENABLED: { label: '启用', class: 'status-enabled' },
  DISABLED: { label: '停用', class: 'status-disabled' }
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const result = unwrap<PageData<User>>(
      await api.get('/users', {
        params: {
          page: pagination.page,
          pageSize: pagination.pageSize,
          keyword: search.keyword || undefined,
          status: search.status || undefined
        }
      })
    )
    users.value = result.items
    pagination.total = result.total
  } catch (e: any) {
    console.error('加载用户列表失败', e)
    error.value = e?.message || '加载用户列表失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function refresh() {
  pagination.page = 1
  load()
}

// 监听筛选条件变化，自动触发查询
let keywordTimer: ReturnType<typeof setTimeout> | null = null
watch(
  () => search.keyword,
  () => {
    if (keywordTimer) clearTimeout(keywordTimer)
    keywordTimer = setTimeout(() => refresh(), 300)
  }
)
watch(() => search.status, () => refresh())

function openCreateDialog() {
  Object.assign(form, {
    username: '',
    displayName: '',
    password: '123456',
    role: 'STUDENT'
  })
  dialog.value = true
}

async function create() {
  if (!form.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!form.displayName.trim()) {
    ElMessage.warning('请输入姓名')
    return
  }
  if (!form.password || form.password.length < 6) {
    ElMessage.warning('初始密码至少 6 位')
    return
  }
  submitting.value = true
  try {
    await api.post('/users', {
      username: form.username.trim(),
      displayName: form.displayName.trim(),
      password: form.password,
      role: form.role
    })
    ElMessage.success('用户已创建')
    dialog.value = false
    refresh()
  } catch (e) {
    console.error('创建用户失败', e)
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row: User) {
  // 保护当前登录账号，避免误停用后无法登录
  if (row.id === auth.user?.id && row.status === 'ENABLED') {
    ElMessage.warning('不能停用当前登录的账号')
    return
  }
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const action = newStatus === 'ENABLED' ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}账号「${row.displayName}（@${row.username}）」吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await api.patch(`/users/${row.id}/status`, { status: newStatus })
    ElMessage.success(`${action}成功`)
    load()
  } catch {
    // 用户取消操作，静默处理
  }
}

async function removeUser(row: User) {
  if (row.id === auth.user?.id) {
    ElMessage.warning('不能删除当前登录账号')
    return
  }
  try {
    await ElMessageBox.confirm(
      `删除账号「${row.displayName}（@${row.username}）」后将无法登录，并从用户列表中移除；其历史业务记录会保留。是否继续？`,
      '确认删除账号',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    deletingUserId.value = row.id
    await api.delete(`/users/${row.id}`)
    if (users.value.length === 1 && pagination.page > 1) {
      pagination.page -= 1
    }
    ElMessage.success('用户已删除')
    await load()
  } catch (action) {
    if (action !== 'cancel' && action !== 'close') {
      console.error('删除用户失败', action)
    }
  } finally {
    deletingUserId.value = null
  }
}

function openResetDialog(row: User) {
  resetTarget.value = row
  resetForm.newPassword = ''
  resetDialog.value = true
}

async function resetPassword() {
  if (!resetTarget.value) return
  if (!resetForm.newPassword || resetForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  resetSubmitting.value = true
  try {
    await api.post(`/users/${resetTarget.value.id}/reset-password`, {
      newPassword: resetForm.newPassword
    })
    ElMessage.success(`已重置「${resetTarget.value.displayName}」的密码`)
    resetDialog.value = false
  } catch (e) {
    console.error('重置密码失败', e)
  } finally {
    resetSubmitting.value = false
  }
}

function formatDateTime(s?: string) {
  if (!s) return '—'
  const d = new Date(s)
  if (isNaN(d.getTime())) return s
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(load)
</script>

<template>
  <div class="users-page">
    <div class="page-header-row">
      <div>
        <h2>用户与权限</h2>
        <p>管理员与学生账号管理</p>
      </div>
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增用户</el-button>
      </div>
    </div>

    <div class="search-bar">
      <div class="search-group">
        <el-input
          v-model="search.keyword"
          :prefix-icon="Search"
          placeholder="搜索姓名或用户名"
          clearable
          @keyup.enter="refresh"
        />
      </div>
      <div class="filter-group">
        <el-select v-model="search.status" placeholder="状态" clearable style="width: 130px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button :icon="Refresh" @click="refresh">刷新</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <h3>账号列表</h3>
        <span class="count">共 {{ pagination.total }} 个账号</span>
      </div>

      <div class="table-wrapper">
        <template v-if="loading">
          <SkeletonTable :rows="6" :columns="6" />
        </template>
        <template v-else-if="error">
          <ErrorState
            title="加载失败"
            description="用户列表加载失败，请检查网络连接后重试"
            :error="error"
            @retry="load"
          />
        </template>
        <template v-else-if="users.length === 0">
          <EmptyState
            title="暂无用户数据"
            description="点击上方「新增用户」创建第一个账号"
          />
        </template>
        <template v-else>
          <table class="data-table">
            <thead>
              <tr>
                <th>姓名</th>
                <th>用户名</th>
                <th>角色</th>
                <th>状态</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in users" :key="row.id">
                <td class="name-cell">
                  <UserIcon class="icon" />
                  <strong>{{ row.displayName }}</strong>
                </td>
                <td class="username-cell">@{{ row.username }}</td>
                <td>
                  <span :class="['role-tag', roleMap[row.role]?.class]">
                    {{ roleMap[row.role]?.label || row.role }}
                  </span>
                </td>
                <td>
                  <span :class="['status-badge', statusMap[row.status]?.class]">
                    {{ statusMap[row.status]?.label || row.status }}
                  </span>
                </td>
                <td class="time-cell">{{ formatDateTime(row.createdAt) }}</td>
                <td class="actions-cell">
                  <div class="actions">
                    <button
                      class="action-btn reset"
                      title="重置密码"
                      @click="openResetDialog(row)"
                    >
                      <Key />
                    </button>
                    <button
                      class="action-btn toggle"
                      :title="row.status === 'ENABLED' ? '停用' : '启用'"
                      @click="toggleStatus(row)"
                    >
                      <Close v-if="row.status === 'ENABLED'" />
                      <Check v-else />
                    </button>
                    <button
                      v-if="row.id !== auth.user?.id"
                      class="action-btn delete"
                      title="删除用户"
                      :disabled="deletingUserId === row.id"
                      @click="removeUser(row)"
                    >
                      <Delete />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </template>
      </div>

      <div class="pagination-bar" v-if="pagination.total > 0">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 15, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <!-- 新增用户 -->
    <el-dialog
      v-model="dialog"
      title="新增用户"
      width="min(460px, 92vw)"
      destroy-on-close
    >
      <el-form label-position="top">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.displayName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="初始密码" required>
          <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="角色">
          <el-segmented
            v-model="form.role"
            :options="[
              { label: '学生', value: 'STUDENT' },
              { label: '教师 / 管理员', value: 'ADMIN' }
            ]"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="create">创建</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog
      v-model="resetDialog"
      title="重置密码"
      width="min(440px, 92vw)"
      destroy-on-close
    >
      <p class="reset-tip" v-if="resetTarget">
        将为 <strong>{{ resetTarget.displayName }}</strong>（@{{ resetTarget.username }}）设置新密码
      </p>
      <el-form label-position="top">
        <el-form-item label="新密码" required>
          <el-input
            v-model="resetForm.newPassword"
            type="password"
            show-password
            placeholder="至少 6 位"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialog = false">取消</el-button>
        <el-button type="primary" :loading="resetSubmitting" @click="resetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.users-page {
  width: 100%;
}

.page-header-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}

.page-header-row h2 {
  margin: 0 0 4px;
  font-size: 20px;
}

.page-header-row p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  padding: 16px;
  background: white;
  border: 1px solid var(--line);
  border-radius: 6px;
}

.search-group {
  flex: 1;
  max-width: 520px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.panel {
  background: white;
  border: 1px solid var(--line);
  border-radius: 6px;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid var(--line);
}

.panel-header h3 {
  margin: 0;
  font-size: 15px;
}

.count {
  color: var(--muted);
  font-size: 13px;
}

.table-wrapper {
  position: relative;
  overflow-x: auto;
}

.data-table {
  width: 100%;
  min-width: 720px;
  border-collapse: collapse;
  font-size: 13px;
  table-layout: fixed;
}

.data-table th,
.data-table td {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.data-table th {
  background: var(--green-light);
  padding: 14px 16px;
  text-align: left;
  font-weight: 600;
  color: var(--green-dark);
  border-bottom: 2px solid var(--line);
}

.data-table td {
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
  color: var(--ink);
  vertical-align: middle;
}

/* 列宽分配 */
.data-table th:nth-child(1),
.data-table td:nth-child(1) { width: 18%; }
.data-table th:nth-child(2),
.data-table td:nth-child(2) { width: 18%; }
.data-table th:nth-child(3),
.data-table td:nth-child(3) { width: 14%; }
.data-table th:nth-child(4),
.data-table td:nth-child(4) { width: 10%; }
.data-table th:nth-child(5),
.data-table td:nth-child(5) { width: 20%; }
.data-table th:nth-child(6),
.data-table td:nth-child(6) { width: 20%; }

.data-table tbody tr:hover {
  background: var(--green-light);
}

.name-cell {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.name-cell .icon {
  color: var(--green);
  font-size: 16px;
  width: 16px;
  height: 16px;
  vertical-align: middle;
  margin-right: 8px;
}

.name-cell strong {
  vertical-align: middle;
}

.username-cell {
  color: var(--muted);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12.5px;
}

.role-tag {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.role-admin {
  background: #fff5e6;
  color: var(--amber);
}

.role-student {
  background: var(--green-light);
  color: var(--green);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.status-enabled {
  background: #edf4ef;
  color: var(--green);
}

.status-disabled {
  background: #f8f0ef;
  color: var(--red);
}

.time-cell {
  color: var(--muted);
  white-space: nowrap;
}

.actions-cell {
  white-space: nowrap;
}

.actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: 1px solid var(--line);
  border-radius: 5px;
  background: white;
  color: var(--muted);
  font-size: 14px;
  transition: all 0.2s;
}

.action-btn :deep(svg) {
  width: 14px;
  height: 14px;
}

.action-btn:hover {
  border-color: var(--green);
  color: var(--green);
  background: var(--green-light);
}

.action-btn.reset:hover {
  border-color: var(--amber);
  color: var(--amber);
  background: #fff5e6;
}

.action-btn.toggle:hover {
  border-color: var(--red);
  color: var(--red);
  background: #f8f0ef;
}

.action-btn.delete:hover {
  border-color: var(--red);
  color: var(--red);
  background: #fdf0f0;
}

.loading-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.8);
  color: var(--muted);
  font-size: 13px;
}

.loading-overlay .is-loading {
  font-size: 22px;
  color: var(--green);
}

.loading-overlay .is-loading svg {
  width: 22px;
  height: 22px;
}

.empty-state {
  padding: 48px 24px;
  text-align: center;
  color: var(--muted);
}

.empty-icon {
  font-size: 48px;
  color: var(--line);
  margin-bottom: 12px;
}

.empty-icon svg {
  width: 48px;
  height: 48px;
}

.empty-state p {
  margin: 0 0 8px;
  font-size: 14px;
}

.empty-hint {
  font-size: 12px !important;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 16px 18px;
  border-top: 1px solid var(--line);
}

.reset-tip {
  margin: 0 0 16px;
  padding: 10px 12px;
  background: var(--green-light);
  border-radius: 6px;
  font-size: 13px;
  color: var(--green-dark);
}

.reset-tip strong {
  color: var(--green);
}

@media (max-width: 1000px) {
  .search-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-group {
    max-width: none;
  }

  .filter-group {
    justify-content: flex-end;
  }
}

@media (max-width: 760px) {
  .page-header-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
