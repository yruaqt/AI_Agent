<script setup lang="ts">
import { onMounted, ref, reactive, computed, watch } from 'vue'
import api, { unwrap } from '@/api'
import type { Orchard, PageData, Task } from '@/types'
import { useAuthStore } from '@/stores/auth'
import {
  MagicStick,
  Refresh,
  View,
  Edit,
  ArrowRight,
  WarningFilled,
  InfoFilled,
  Timer,
  Document,
  Filter,
  Check
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const auth = useAuthStore()

// 数据
const orchards = ref<Orchard[]>([])
const orchardId = ref<string>('')
const tasks = ref<Task[]>([])
const loading = ref(false)
const generating = ref(false)
const date = ref(new Date().toISOString().slice(0, 10))
const statusFilter = ref('')

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 15,
  total: 0
})

// 生成结果展示
const lastGenerated = ref<{
  batchId?: string
  weatherSummary?: string
  phenology?: string
} | null>(null)

const currentOrchard = computed(() => orchards.value.find(o => o.id === orchardId.value) || null)

// 详情抽屉
const detailVisible = ref(false)
const currentTask = ref<Task | null>(null)

// 编辑弹窗
const editVisible = ref(false)
const editForm = ref<Partial<Task>>({})
const editLoading = ref(false)

// 状态流转备注
const statusRemark = ref('')
const statusRemarkVisible = ref(false)
const pendingStatusChange = ref<{ task: Task; status: string } | null>(null)

// 常量映射
const statusText: Record<string, string> = {
  DRAFT: '待确认',
  CONFIRMED: '已确认',
  TODO: '待执行',
  DOING: '执行中',
  DONE: '已完成',
  CANCELLED: '已取消'
}

const statusTagType = (status: string): any => {
  const map: Record<string, any> = {
    DRAFT: 'info',
    CONFIRMED: 'primary',
    TODO: 'warning',
    DOING: '',
    DONE: 'success',
    CANCELLED: 'danger'
  }
  return map[status] || ''
}

const priorityText: Record<string, string> = {
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低'
}

const typeText: Record<string, string> = {
  DRAINAGE_CHECK: '排水检查',
  IRRIGATION: '灌溉',
  FERTILIZATION: '施肥',
  PEST_CONTROL: '病虫害防治',
  PRUNING: '修剪',
  WEEDING: '除草',
  MONITORING: '巡园监测',
  HARVEST: '采收',
  POST_HARVEST: '采后管理',
  OTHER: '其他'
}

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

// 统计（基于当前页数据）
const statusCounts = computed(() => {
  const counts: Record<string, number> = {}
  Object.keys(statusText).forEach(s => { counts[s] = 0 })
  tasks.value.forEach(t => {
    if (counts[t.status] !== undefined) counts[t.status]++
  })
  return counts
})

// 加载数据
async function load() {
  loading.value = true
  try {
    // 首次加载果园列表
    if (orchards.value.length === 0) {
      const d = unwrap<PageData<Orchard>>(
        await api.get('/orchards', { params: { pageSize: 50, status: 'ENABLED' } })
      )
      orchards.value = d.items
      if (!orchardId.value && orchards.value[0]) {
        orchardId.value = orchards.value[0].id
      }
    }
    if (!orchardId.value) {
      tasks.value = []
      pagination.total = 0
      return
    }
    const params: Record<string, any> = {
      orchardId: orchardId.value,
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (date.value) params.date = date.value
    if (statusFilter.value) params.status = statusFilter.value

    const result = unwrap<PageData<Task>>(await api.get('/tasks', { params }))
    tasks.value = result.items
    pagination.total = result.total
  } finally {
    loading.value = false
  }
}

// 重置到第一页并刷新
function refresh() {
  pagination.page = 1
  load()
}

// 生成任务
async function generate() {
  if (!orchardId.value) {
    ElMessage.warning('请先选择果园')
    return
  }
  generating.value = true
  try {
    const res = unwrap<any>(
      await api.post(`/orchards/${orchardId.value}/tasks/generate`, {
        date: date.value,
        focus: '',
        saveAsDraft: true
      })
    )
    lastGenerated.value = {
      batchId: res.batchId,
      weatherSummary: res.weatherSummary,
      phenology: res.phenology
    }
    ElMessage.success('农事任务已生成')
    await refresh()
  } finally {
    generating.value = false
  }
}

// 查看详情
function openDetail(task: Task) {
  currentTask.value = task
  detailVisible.value = true
}

// 打开编辑
function openEdit(task: Task) {
  if (!auth.isAdmin) {
    ElMessage.warning('只有管理员可以编辑任务')
    return
  }
  editForm.value = { ...task }
  editVisible.value = true
}

// 保存编辑
async function saveEdit() {
  if (!editForm.value.id) return
  editLoading.value = true
  try {
    await api.put(`/tasks/${editForm.value.id}`, {
      title: editForm.value.title,
      content: editForm.value.content,
      priority: editForm.value.priority,
      suggestedTime: editForm.value.suggestedTime,
      safetyNotice: editForm.value.safetyNotice
    })
    ElMessage.success('任务已更新')
    editVisible.value = false
    await load()
  } finally {
    editLoading.value = false
  }
}

// 状态流转前置检查
function canChangeStatus(task: Task, status: string): boolean {
  // 学生只能按固定流程流转
  if (auth.isStudent) {
    const allowed: Record<string, string[]> = {
      CONFIRMED: ['TODO', 'DOING'],
      TODO: ['DOING'],
      DOING: ['DONE']
    }
    return allowed[task.status]?.includes(status) || false
  }
  // 管理员可以操作所有状态
  return auth.isAdmin
}

// 触发状态变更
function requestStatusChange(task: Task, status: string) {
  if (!canChangeStatus(task, status)) {
    ElMessage.warning('当前角色不允许该状态流转')
    return
  }
  pendingStatusChange.value = { task, status }
  statusRemark.value = ''
  statusRemarkVisible.value = true
}

// 确认状态变更
async function confirmStatusChange() {
  if (!pendingStatusChange.value) return
  const { task, status } = pendingStatusChange.value
  try {
    await api.patch(`/tasks/${task.id}/status`, {
      status,
      remark: statusRemark.value || undefined
    })
    task.status = status as any
    ElMessage.success('状态已更新')
    statusRemarkVisible.value = false
    pendingStatusChange.value = null
    await load()
  } catch {
    // api 拦截器已处理错误提示
  }
}

// 获取可流转的状态选项
function getStatusOptions(task: Task) {
  const all = [
    { value: 'CONFIRMED', label: '确认' },
    { value: 'TODO', label: '待执行' },
    { value: 'DOING', label: '执行中' },
    { value: 'DONE', label: '完成' },
    { value: 'CANCELLED', label: '取消' }
  ]
  // 当前状态不需要显示
  return all.filter(s => s.value !== task.status)
}

// 监听筛选条件变化：日期、状态变化重置到第一页；果园切换也重置
watch(date, () => refresh())
watch(statusFilter, () => refresh())
watch(orchardId, () => refresh())

onMounted(load)
</script>

<template>
  <div>
    <!-- 页面标题栏 -->
    <div class="page-title-row">
      <div>
        <h2>{{ date }} 农事安排</h2>
        <p>{{ currentOrchard?.name || '请选择果园' }} · 共 {{ pagination.total }} 项任务</p>
      </div>
      <div class="toolbar tasks-toolbar">
        <el-select
          v-model="orchardId"
          placeholder="选择果园"
          style="width: 200px"
        >
          <el-option
            v-for="o in orchards"
            :key="o.id"
            :label="o.name"
            :value="o.id"
          />
        </el-select>
        <el-date-picker
          v-model="date"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
        />
        <el-button :icon="Refresh" @click="refresh">刷新</el-button>
        <el-button
          type="primary"
          :icon="MagicStick"
          :loading="generating"
          @click="generate"
        >
          Agent 生成任务
        </el-button>
      </div>
    </div>

    <!-- 生成结果摘要 -->
    <div v-if="lastGenerated" class="panel" style="margin-bottom: 18px; padding: 14px 18px;">
      <div style="display: flex; align-items: center; gap: 16px; flex-wrap: wrap; font-size: 13px; color: var(--muted);">
        <span v-if="lastGenerated.weatherSummary">
          <el-icon style="vertical-align: -2px; margin-right: 4px;"><WarningFilled /></el-icon>
          天气：{{ lastGenerated.weatherSummary }}
        </span>
        <span v-if="lastGenerated.phenology">
          <el-icon style="vertical-align: -2px; margin-right: 4px;"><Timer /></el-icon>
          物候期：{{ phenologyNames[lastGenerated.phenology] || lastGenerated.phenology }}
        </span>
        <span v-if="lastGenerated.batchId">
          <el-icon style="vertical-align: -2px; margin-right: 4px;"><Document /></el-icon>
          批次：{{ lastGenerated.batchId }}
        </span>
        <el-button link size="small" @click="lastGenerated = null" style="margin-left: auto;">清除</el-button>
      </div>
    </div>

    <!-- 状态统计卡片（基于当前页统计，点击切换状态筛选） -->
    <div class="stat-grid" style="grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); margin-bottom: 18px;">
      <div
        v-for="(label, key) in statusText"
        :key="key"
        class="stat-card"
        :class="{ active: statusFilter === key }"
        @click="statusFilter = statusFilter === key ? '' : key"
        style="cursor: pointer; min-height: 80px;"
      >
        <span class="label">{{ label }}</span>
        <strong :style="{ color: 'var(--green)' }">{{ statusCounts[key] || 0 }}</strong>
        <span class="trend" style="font-size: 10px;">当前页</span>
      </div>
    </div>

    <!-- 筛选标签 -->
    <div v-if="statusFilter" style="margin-bottom: 12px;">
      <el-tag closable @close="statusFilter = ''" type="info" size="small">
        <el-icon style="vertical-align: -2px; margin-right: 4px;"><Filter /></el-icon>
        筛选：{{ statusText[statusFilter] }}
      </el-tag>
    </div>

    <!-- 任务列表 -->
    <section class="panel" v-loading="loading">
      <el-empty v-if="tasks.length === 0" description="暂无任务" style="padding: 48px 0;" />
      <el-table v-else :data="tasks" stripe>
        <el-table-column label="优先级" width="88">
          <template #default="{ row }">
            <span :class="`priority-${row.priority.toLowerCase()}`">
              ● {{ priorityText[row.priority] || row.priority }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="任务" min-width="170">
          <template #default="{ row }">
            <strong>{{ row.title }}</strong>
            <div class="cell-sub">
              {{ typeText[row.type] || row.type }} · {{ row.suggestedTime }}
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="content" label="执行内容" min-width="260" show-overflow-tooltip />

        <el-table-column label="依据" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.basis">{{ row.basis }}</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain" size="small">
              {{ statusText[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="auth.isAdmin"
              size="small"
              link
              :icon="Edit"
              @click="openEdit(row)"
            >编辑</el-button>
            <el-dropdown
              @command="(s: string) => requestStatusChange(row, s)"
              style="margin-left: 8px;"
            >
              <el-button size="small">
                流转<el-icon class="el-icon--right"><ArrowRight /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="opt in getStatusOptions(row)"
                    :key="opt.value"
                    :command="opt.value"
                    :disabled="!canChangeStatus(row, opt.value)"
                  >
                    {{ opt.label }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

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
    </section>

    <!-- 任务详情抽屉 -->
    <el-drawer v-model="detailVisible" title="任务详情" size="min(460px, 92vw)" destroy-on-close>
      <div v-if="currentTask" class="task-detail">
        <div class="detail-header">
          <el-tag :type="statusTagType(currentTask.status)" size="small">
            {{ statusText[currentTask.status] }}
          </el-tag>
          <span :class="`priority-${currentTask.priority.toLowerCase()}`" style="margin-left: 8px; font-size: 13px;">
            {{ priorityText[currentTask.priority] }}优先级
          </span>
        </div>

        <h3 style="margin: 12px 0 8px; font-size: 18px;">{{ currentTask.title }}</h3>

        <div class="detail-section">
          <div class="detail-label"><el-icon><Document /></el-icon> 任务类型</div>
          <div class="detail-value">{{ typeText[currentTask.type] || currentTask.type }}</div>
        </div>

        <div class="detail-section">
          <div class="detail-label"><el-icon><Timer /></el-icon> 建议执行时间</div>
          <div class="detail-value">{{ currentTask.suggestedTime || '—' }}</div>
        </div>

        <div class="detail-section">
          <div class="detail-label"><el-icon><InfoFilled /></el-icon> 执行内容</div>
          <div class="detail-value" style="line-height: 1.7;">{{ currentTask.content }}</div>
        </div>

        <div class="detail-section">
          <div class="detail-label"><el-icon><Check /></el-icon> 任务依据</div>
          <div class="detail-value" style="line-height: 1.7;">{{ currentTask.basis || '—' }}</div>
        </div>

        <div v-if="currentTask.safetyNotice" class="detail-section">
          <div class="detail-label" style="color: var(--red);">
            <el-icon><WarningFilled /></el-icon> 安全提示
          </div>
          <div class="detail-value" style="color: var(--red); line-height: 1.7;">
            {{ currentTask.safetyNotice }}
          </div>
        </div>

        <div class="detail-actions" style="margin-top: 24px; padding-top: 16px; border-top: 1px solid var(--line);">
          <el-button
            v-if="auth.isAdmin"
            type="primary"
            :icon="Edit"
            @click="detailVisible = false; openEdit(currentTask!)"
          >
            编辑任务
          </el-button>
          <el-dropdown @command="(s: string) => requestStatusChange(currentTask!, s)">
            <el-button>
              变更状态<el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="opt in getStatusOptions(currentTask)"
                  :key="opt.value"
                  :command="opt.value"
                  :disabled="!canChangeStatus(currentTask, opt.value)"
                >
                  {{ opt.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-drawer>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑任务" width="min(560px, 92vw)" destroy-on-close>
      <el-form v-if="editForm" label-width="80px" style="margin-top: 8px;">
        <el-form-item label="任务标题">
          <el-input v-model="editForm.title" placeholder="请输入任务标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="执行内容">
          <el-input
            v-model="editForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入执行内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="editForm.priority">
            <el-radio-button label="HIGH">高</el-radio-button>
            <el-radio-button label="MEDIUM">中</el-radio-button>
            <el-radio-button label="LOW">低</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="建议时间">
          <el-input v-model="editForm.suggestedTime" placeholder="例如：上午 8:00-10:00" />
        </el-form-item>
        <el-form-item label="安全提示">
          <el-input
            v-model="editForm.safetyNotice"
            type="textarea"
            :rows="2"
            placeholder="请输入安全提示"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 状态流转备注弹窗 -->
    <el-dialog v-model="statusRemarkVisible" title="状态变更备注" width="min(420px, 92vw)" destroy-on-close>
      <p style="margin: 0 0 12px; font-size: 13px; color: var(--muted);">
        将任务 <strong>{{ pendingStatusChange?.task.title }}</strong>
        变更为 <strong>{{ statusText[pendingStatusChange?.status || ''] }}</strong>
      </p>
      <el-input
        v-model="statusRemark"
        type="textarea"
        :rows="3"
        placeholder="请输入备注（可选）"
        maxlength="200"
        show-word-limit
      />
      <template #footer>
        <el-button @click="statusRemarkVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmStatusChange">确认变更</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.cell-sub {
  font-size: 11px;
  color: var(--muted);
  margin-top: 4px;
}

.priority-high {
  color: var(--red);
}

.priority-medium {
  color: var(--amber);
}

.priority-low {
  color: var(--green);
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 16px 18px;
  border-top: 1px solid var(--line);
}

.stat-card.active {
  border-color: var(--green);
  box-shadow: 0 0 0 1px var(--green);
}

.task-detail {
  padding: 4px 8px;
}

.detail-header {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}

.detail-section {
  margin-bottom: 16px;
}

.detail-label {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.detail-value {
  font-size: 14px;
  color: var(--ink);
  word-break: break-word;
}

.detail-actions {
  display: flex;
  gap: 10px;
}

.muted {
  color: var(--muted);
}

/* 移动端适配 */
@media (max-width: 760px) {
  .tasks-toolbar {
    flex-wrap: wrap;
  }

  .tasks-toolbar .el-date-editor {
    width: 100% !important;
    margin-bottom: 4px;
  }

  .tasks-toolbar .el-button {
    flex: 1;
  }

  .stat-grid {
    grid-template-columns: repeat(3, 1fr) !important;
    gap: 6px !important;
    margin-bottom: 14px !important;
  }

  .stat-card {
    min-height: 68px !important;
    padding: 10px 8px !important;
  }

  .stat-card .label {
    font-size: 10px;
    line-height: 1.3;
  }

  .stat-card strong {
    font-size: 16px !important;
  }

  /* 表格横向滚动 */
  :deep(.el-table) {
    width: 100% !important;
    overflow-x: auto;
  }

  :deep(.el-table__inner-wrapper) {
    overflow-x: auto;
  }

  /* 表单标签置顶 */
  :deep(.el-form--inline .el-form-item),
  :deep(.el-form-item) {
    margin-bottom: 12px;
  }

  .detail-actions {
    flex-direction: column;
    gap: 8px;
  }

  .detail-actions .el-button {
    width: 100%;
    margin-left: 0 !important;
  }
}
</style>
