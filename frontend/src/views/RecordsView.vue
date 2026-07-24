<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import api, { unwrap } from '@/api'
import type { Orchard, PageData, TrainingRecord, TrainingRecordCreate, TrainingRecordReview, Task } from '@/types'
import { useAuthStore } from '@/stores/auth'
import { Plus, Refresh, View, Edit, Check, Filter, Star } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import SkeletonTable from '@/components/SkeletonTable.vue'

const auth = useAuthStore()

const orchards = ref<Orchard[]>([])
const records = ref<TrainingRecord[]>([])
const tasks = ref<Task[]>([])
const loading = ref(false)

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 15,
  total: 0
})

const dialog = ref(false)
const detailVisible = ref(false)
const editVisible = ref(false)
const reviewVisible = ref(false)

const currentRecord = ref<TrainingRecord | null>(null)
const editRecord = ref<TrainingRecord | null>(null)
const reviewRecord = ref<TrainingRecord | null>(null)

const searchForm = reactive({
  orchardId: '',
  startDate: '',
  endDate: '',
  studentId: ''
})

const form = reactive<TrainingRecordCreate>({
  orchardId: '',
  recordDate: new Date().toISOString().slice(0, 10),
  inspectedTreeCount: 30,
  abnormalTreeCount: 0,
  phenomenon: '',
  measure: '',
  result: ''
})

const reviewForm = reactive<TrainingRecordReview>({
  score: 85,
  comment: '',
  status: 'APPROVED'
})

const statusText: Record<string, string> = {
  PENDING: '待评价',
  APPROVED: '已通过',
  REJECTED: '已退回'
}

const statusTagType = (status: string): any => {
  const map: Record<string, any> = {
    PENDING: 'info',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || ''
}

async function load() {
  loading.value = true
  try {
    // 首次加载果园列表
    if (orchards.value.length === 0) {
      const o = unwrap<PageData<Orchard>>(
        await api.get('/orchards', { params: { pageSize: 50, status: 'ENABLED' } })
      )
      orchards.value = o.items
      if (!searchForm.orchardId && orchards.value[0]) {
        searchForm.orchardId = orchards.value[0].id
        form.orchardId = orchards.value[0].id
      }
    }

    // 加载关联任务列表（用于新增记录时选择）
    if (searchForm.orchardId && tasks.value.length === 0) {
      tasks.value = unwrap<PageData<Task>>(
        await api.get('/tasks', {
          params: { orchardId: searchForm.orchardId, pageSize: 50 }
        })
      ).items
    }

    const params: Record<string, any> = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (searchForm.orchardId) params.orchardId = searchForm.orchardId
    if (searchForm.startDate) params.startDate = searchForm.startDate
    if (searchForm.endDate) params.endDate = searchForm.endDate
    if (searchForm.studentId) params.studentId = searchForm.studentId

    const result = unwrap<PageData<TrainingRecord>>(
      await api.get('/training-records', { params })
    )
    records.value = result.items
    pagination.total = result.total
  } finally {
    loading.value = false
  }
}

function refresh() {
  pagination.page = 1
  load()
}

function handleSearch() {
  refresh()
}

function resetSearch() {
  searchForm.startDate = ''
  searchForm.endDate = ''
  searchForm.studentId = ''
  refresh()
}

// 监听筛选条件变化自动触发查询
watch(
  () => searchForm.orchardId,
  () => {
    form.orchardId = searchForm.orchardId
    refresh()
  }
)
watch(() => searchForm.startDate, () => refresh())
watch(() => searchForm.endDate, () => refresh())
watch(() => searchForm.studentId, () => refresh())

async function submit() {
  await api.post('/training-records', form)
  ElMessage.success('实训记录已提交')
  dialog.value = false
  resetForm()
  await load()
}

function resetForm() {
  form.recordDate = new Date().toISOString().slice(0, 10)
  form.inspectedTreeCount = 30
  form.abnormalTreeCount = 0
  form.phenomenon = ''
  form.measure = ''
  form.result = ''
}

function openDetail(record: TrainingRecord) {
  currentRecord.value = record
  detailVisible.value = true
}

function openEdit(record: TrainingRecord) {
  if (!auth.isAdmin && record.studentId !== auth.user?.id) {
    ElMessage.warning('您只能编辑自己的实训记录')
    return
  }
  if (record.status === 'APPROVED') {
    ElMessage.warning('教师已评价的记录无法修改')
    return
  }
  editRecord.value = { ...record }
  editVisible.value = true
}

async function saveEdit() {
  if (!editRecord.value?.id) return
  try {
    await api.put(`/training-records/${editRecord.value.id}`, {
      recordDate: editRecord.value.recordDate,
      inspectedTreeCount: editRecord.value.inspectedTreeCount,
      abnormalTreeCount: editRecord.value.abnormalTreeCount,
      phenomenon: editRecord.value.phenomenon,
      measure: editRecord.value.measure,
      result: editRecord.value.result
    })
    ElMessage.success('实训记录已更新')
    editVisible.value = false
    await load()
  } catch {
    // api 拦截器已处理错误提示
  }
}

function openReview(record: TrainingRecord) {
  if (!auth.isAdmin) {
    ElMessage.warning('只有教师可以评价实训记录')
    return
  }
  reviewRecord.value = record
  reviewForm.score = record.score || 85
  reviewForm.comment = record.teacherComment || ''
  reviewForm.status = record.status === 'REJECTED' ? 'REJECTED' : 'APPROVED'
  reviewVisible.value = true
}

async function submitReview() {
  if (!reviewRecord.value?.id) return
  try {
    await api.post(`/training-records/${reviewRecord.value.id}/review`, reviewForm)
    ElMessage.success('评价已提交')
    reviewVisible.value = false
    await load()
  } catch {
    // api 拦截器已处理错误提示
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-title-row">
      <div>
        <h2>实训记录</h2>
        <p>共 {{ pagination.total }} 条记录</p>
      </div>
      <div class="toolbar">
        <el-button :icon="Refresh" @click="refresh">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="dialog = true">新增记录</el-button>
      </div>
    </div>

    <section class="panel" style="margin-bottom: 18px;">
      <div class="panel-body">
        <div class="filter-row">
          <el-select
            v-model="searchForm.orchardId"
            placeholder="选择果园"
            clearable
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
            v-model="searchForm.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="开始日期"
          />
          <el-date-picker
            v-model="searchForm.endDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="结束日期"
          />
          <el-input
            v-model="searchForm.studentId"
            placeholder="学生ID筛选（管理员）"
            clearable
            style="width: 200px"
            :disabled="auth.isStudent"
          />
          <el-button :icon="Filter" @click="handleSearch">筛选</el-button>
          <el-button link @click="resetSearch">重置</el-button>
        </div>
      </div>
    </section>

    <section class="panel">
      <template v-if="loading">
        <SkeletonTable :rows="6" :columns="10" />
      </template>
      <template v-else>
        <el-empty v-if="records.length === 0" description="暂无实训记录" style="padding: 48px 0;" />
        <el-table v-else :data="records" stripe>
          <el-table-column prop="recordDate" label="日期" width="120" />
          <el-table-column label="抽查数据" width="150">
            <template #default="{ row }">
              <strong>{{ row.inspectedTreeCount || 0 }}</strong> 株 / 异常
              <span class="priority-high">{{ row.abnormalTreeCount || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="phenomenon" label="现场现象" min-width="240" show-overflow-tooltip />
          <el-table-column prop="measure" label="处理措施" min-width="220" show-overflow-tooltip />
          <el-table-column label="提交人" width="100">
            <template #default="{ row }">
              {{ row.studentName || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status || 'PENDING')" effect="plain" size="small">
                {{ statusText[row.status || 'PENDING'] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="评分" width="90">
            <template #default="{ row }">
              <span v-if="row.score !== undefined" class="score">
                <el-icon style="color: var(--amber);"><Star /></el-icon>
                {{ row.score }}
              </span>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="teacherComment" label="教师评语" min-width="180" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="提交时间" width="180" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" link :icon="View" @click="openDetail(row)">详情</el-button>
              <el-button
                v-if="(auth.isAdmin || row.studentId === auth.user?.id) && row.status !== 'APPROVED'"
                size="small"
                link
                :icon="Edit"
                @click="openEdit(row)"
              >编辑</el-button>
              <el-button
                v-if="auth.isAdmin"
                size="small"
                link
                :icon="Check"
                @click="openReview(row)"
              >评价</el-button>
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
      </template>
    </section>

    <el-dialog v-model="dialog" title="新增实训记录" width="min(560px, 92vw)" destroy-on-close>
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="日期">
            <el-date-picker v-model="form.recordDate" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item label="抽查株数">
            <el-input-number v-model="form.inspectedTreeCount" :min="1" />
          </el-form-item>
          <el-form-item label="异常株数">
            <el-input-number v-model="form.abnormalTreeCount" :min="0" :max="form.inspectedTreeCount" />
          </el-form-item>
        </div>
        <el-form-item label="关联任务">
          <el-select v-model="form.taskId" placeholder="选择任务（可选）" style="width: 100%;">
            <el-option
              v-for="task in tasks"
              :key="task.id"
              :label="task.title"
              :value="task.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="现场现象">
          <el-input v-model="form.phenomenon" type="textarea" :rows="3" placeholder="描述观察到的现象" />
        </el-form-item>
        <el-form-item label="处理措施">
          <el-input v-model="form.measure" type="textarea" :rows="3" placeholder="描述采取的措施" />
        </el-form-item>
        <el-form-item label="处理结果">
          <el-input v-model="form.result" type="textarea" :rows="2" placeholder="描述处理结果（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submit">提交记录</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="实训记录详情" size="min(480px, 92vw)" destroy-on-close>
      <div v-if="currentRecord" class="record-detail">
        <div class="detail-header">
          <el-tag :type="statusTagType(currentRecord.status || 'PENDING')" size="small">
            {{ statusText[currentRecord.status || 'PENDING'] }}
          </el-tag>
          <span v-if="currentRecord.score !== undefined" class="score-badge">
            <el-icon style="color: var(--amber);"><Star /></el-icon>
            {{ currentRecord.score }}分
          </span>
        </div>

        <div class="detail-section">
          <div class="detail-label">日期</div>
          <div class="detail-value">{{ currentRecord.recordDate }}</div>
        </div>

        <div class="detail-section">
          <div class="detail-label">果园</div>
          <div class="detail-value">{{ currentRecord.orchardName || '—' }}</div>
        </div>

        <div class="detail-section">
          <div class="detail-label">关联任务</div>
          <div class="detail-value">{{ currentRecord.taskTitle || '—' }}</div>
        </div>

        <div class="detail-section">
          <div class="detail-label">抽查数据</div>
          <div class="detail-value">
            抽查 <strong>{{ currentRecord.inspectedTreeCount }}</strong> 株，异常
            <strong class="priority-high">{{ currentRecord.abnormalTreeCount }}</strong> 株
          </div>
        </div>

        <div class="detail-section">
          <div class="detail-label">现场现象</div>
          <div class="detail-value" style="line-height: 1.7;">{{ currentRecord.phenomenon }}</div>
        </div>

        <div class="detail-section">
          <div class="detail-label">处理措施</div>
          <div class="detail-value" style="line-height: 1.7;">{{ currentRecord.measure }}</div>
        </div>

        <div class="detail-section" v-if="currentRecord.result">
          <div class="detail-label">处理结果</div>
          <div class="detail-value" style="line-height: 1.7;">{{ currentRecord.result }}</div>
        </div>

        <div class="detail-section">
          <div class="detail-label">提交人</div>
          <div class="detail-value">{{ currentRecord.studentName || '—' }}</div>
        </div>

        <div v-if="currentRecord.teacherComment" class="detail-section review-section">
          <div class="detail-label">
            <el-icon><Star /></el-icon> 教师评语
          </div>
          <div class="detail-value" style="line-height: 1.7;">{{ currentRecord.teacherComment }}</div>
        </div>

        <div class="detail-actions" style="margin-top: 24px; padding-top: 16px; border-top: 1px solid var(--line);">
          <el-button
            v-if="(auth.isAdmin || currentRecord.studentId === auth.user?.id) && currentRecord.status !== 'APPROVED'"
            type="primary"
            :icon="Edit"
            @click="detailVisible = false; openEdit(currentRecord)"
          >
            编辑记录
          </el-button>
          <el-button
            v-if="auth.isAdmin"
            :icon="Check"
            @click="detailVisible = false; openReview(currentRecord)"
          >
            教师评价
          </el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="editVisible" title="编辑实训记录" width="min(560px, 92vw)" destroy-on-close>
      <el-form v-if="editRecord" label-position="top">
        <div class="form-grid">
          <el-form-item label="日期">
            <el-date-picker v-model="editRecord.recordDate" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item label="抽查株数">
            <el-input-number v-model="editRecord.inspectedTreeCount" :min="1" />
          </el-form-item>
          <el-form-item label="异常株数">
            <el-input-number v-model="editRecord.abnormalTreeCount" :min="0" :max="editRecord.inspectedTreeCount" />
          </el-form-item>
        </div>
        <el-form-item label="现场现象">
          <el-input v-model="editRecord.phenomenon" type="textarea" :rows="3" placeholder="描述观察到的现象" />
        </el-form-item>
        <el-form-item label="处理措施">
          <el-input v-model="editRecord.measure" type="textarea" :rows="3" placeholder="描述采取的措施" />
        </el-form-item>
        <el-form-item label="处理结果">
          <el-input v-model="editRecord.result" type="textarea" :rows="2" placeholder="描述处理结果（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存修改</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewVisible" title="教师评价" width="min(500px, 92vw)" destroy-on-close>
      <div v-if="reviewRecord" style="margin-bottom: 16px;">
        <p style="margin: 0 0 12px; font-size: 13px; color: var(--muted);">
          评价记录：<strong>{{ reviewRecord.phenomenon }}</strong>
        </p>
      </div>
      <el-form label-position="top">
        <el-form-item label="评分">
          <div class="score-input">
            <el-slider
              v-model="reviewForm.score"
              :min="0"
              :max="100"
              :step="1"
              show-input
            />
          </div>
        </el-form-item>
        <el-form-item label="评价状态">
          <el-radio-group v-model="reviewForm.status">
            <el-radio-button label="APPROVED">通过</el-radio-button>
            <el-radio-button label="REJECTED">退回</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="评语">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="3"
            placeholder="请输入评语"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 16px 18px;
  border-top: 1px solid var(--line);
}

.priority-high {
  color: var(--red);
  font-weight: 700;
}

.muted {
  color: var(--muted);
}

.score {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--amber);
  font-weight: 600;
}

.record-detail {
  padding: 4px 8px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.score-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: var(--lime-light);
  color: #7a9442;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
}

.detail-section {
  margin-bottom: 16px;
}

.detail-section.review-section {
  background: var(--green-light);
  padding: 12px;
  border-radius: 6px;
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

.score-input {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 12px;
}

.score-input :deep(.el-slider) {
  flex: 1;
}

/* 移动端适配 */
@media (max-width: 760px) {
  .filter-row {
    flex-wrap: wrap;
    gap: 8px;
  }

  .filter-row .el-date-editor {
    flex: 1;
    min-width: 130px;
  }

  .filter-row .el-button {
    flex: 1;
    min-width: 80px;
  }

  /* 表格横向滚动 */
  :deep(.el-table) {
    width: 100% !important;
    overflow-x: auto;
  }

  :deep(.el-table__inner-wrapper) {
    overflow-x: auto;
  }

  .detail-actions {
    flex-direction: column;
    gap: 8px;
  }

  .detail-actions .el-button {
    width: 100%;
    margin-left: 0 !important;
  }

  .score-input {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
}

@media (max-width: 600px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>