<script setup lang="ts">
import { onMounted, onBeforeUnmount, reactive, ref, watch } from 'vue'
import type { UploadRequestOptions, UploadUserFile } from 'element-plus'
import api, { unwrap } from '@/api'
import type {
  Orchard,
  PageData,
  Task,
  TrainingRecord,
  TrainingRecordCreate,
  TrainingRecordImage,
  TrainingRecordReview,
  UploadedFile
} from '@/types'
import { useAuthStore } from '@/stores/auth'
import { Plus, Refresh, View, Edit, Check, Filter, Star, Picture } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import SkeletonTable from '@/components/SkeletonTable.vue'
import ErrorState from '@/components/ErrorState.vue'
import EmptyState from '@/components/EmptyState.vue'

// 图片上传约束（接口文档 12.1）
const MAX_IMAGE_SIZE = 5 * 1024 * 1024 // 单张 5MB
const MAX_IMAGE_COUNT = 6 // 少量现场图片
const IMAGE_ACCEPT = ['image/jpeg', 'image/png', 'image/webp']

const auth = useAuthStore()

const orchards = ref<Orchard[]>([])
const records = ref<TrainingRecord[]>([])
const tasks = ref<Task[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

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
  result: '',
  images: []
})

const reviewForm = reactive<TrainingRecordReview>({
  score: 85,
  comment: '',
  status: 'APPROVED'
})

// 暂存图片：在 TrainingRecordImage 基础上保留 el-upload 的 uid，便于删除时定位
interface StagedImage extends TrainingRecordImage {
  uid: number
}

// 新增对话框中的图片：fileList 供 el-upload 展示，staged 保存后端返回信息
const createImageList = ref<UploadUserFile[]>([])
const createImages = ref<StagedImage[]>([])
const uploadingImage = ref(false)

// 编辑对话框中的图片
const editImageList = ref<UploadUserFile[]>([])
const editImages = ref<StagedImage[]>([])

// 详情抽屉：受控图片 URL 需要带 Authorization 头，转成 blob URL 供 <img> 使用
const detailImageSrc = reactive<Record<string, string>>({})

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
  error.value = null
  // 果园列表单独加载，失败则整页不可用
  try {
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
  } catch (e: any) {
    error.value = '果园数据加载失败：' + (e?.message || '请稍后重试')
    loading.value = false
    return
  }

  // 关联任务列表失败不影响记录展示，仅影响新增时的任务下拉
  if (searchForm.orchardId && tasks.value.length === 0) {
    try {
      tasks.value = unwrap<PageData<Task>>(
        await api.get('/tasks', {
          params: { orchardId: searchForm.orchardId, pageSize: 50 }
        })
      ).items
    } catch {
      tasks.value = []
    }
  }

  // 实训记录列表为主数据，失败时展示错误提示
  try {
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
  } catch (e: any) {
    error.value = '实训记录加载失败：' + (e?.message || '请稍后重试')
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
  // 携带已上传图片引用
  form.images = createImages.value.map(img => ({
    fileId: img.fileId,
    fileName: img.fileName,
    url: img.url
  }))
  try {
    await api.post('/training-records', form)
    ElMessage.success('实训记录已提交')
    dialog.value = false
    resetForm()
    await load()
  } catch {
    // api 拦截器已处理错误提示
  }
}

function resetForm() {
  form.recordDate = new Date().toISOString().slice(0, 10)
  form.inspectedTreeCount = 30
  form.abnormalTreeCount = 0
  form.phenomenon = ''
  form.measure = ''
  form.result = ''
  form.images = []
  createImages.value = []
  createImageList.value = []
}

// 图片上传：客户端校验 + 调用 POST /files/images
function beforeImageUpload(file: File): boolean {
  if (!IMAGE_ACCEPT.includes(file.type)) {
    ElMessage.warning('仅支持 JPEG、PNG、WebP 格式图片')
    return false
  }
  if (file.size > MAX_IMAGE_SIZE) {
    ElMessage.warning('单张图片不能超过 5 MB')
    return false
  }
  return true
}

// el-upload 自定义上传，写入对应图片集合（以 uid 关联 fileList 与暂存数据）
function makeImageUploader(
  imagesRef: typeof createImages,
  fileListRef: typeof createImageList
) {
  return async (options: UploadRequestOptions) => {
    const file = options.file as File
    const uid = options.file.uid
    if (!beforeImageUpload(file)) {
      // 校验失败：从文件列表移除该项
      const idx = fileListRef.value.findIndex(f => f.uid === uid)
      if (idx >= 0) fileListRef.value.splice(idx, 1)
      return
    }
    uploadingImage.value = true
    const fd = new FormData()
    fd.append('file', file)
    try {
      const data = unwrap<UploadedFile>(await api.post('/files/images', fd))
      imagesRef.value.push({
        uid,
        fileId: data.fileId,
        fileName: data.fileName,
        url: data.url
      })
      ElMessage.success('图片上传成功')
    } catch (e) {
      // 上传失败：从文件列表移除，避免展示成功假象
      const idx = fileListRef.value.findIndex(f => f.uid === uid)
      if (idx >= 0) fileListRef.value.splice(idx, 1)
      console.error('上传图片失败', e)
    } finally {
      uploadingImage.value = false
    }
  }
}

const uploadCreateImage = makeImageUploader(createImages, createImageList)
const uploadEditImage = makeImageUploader(editImages, editImageList)

// 删除时按 uid 同步移除 fileList 与暂存数据
function makeImageRemover(
  imagesRef: typeof createImages,
  fileListRef: typeof createImageList
) {
  return (file: UploadUserFile) => {
    const uid = file.uid
    const fIdx = fileListRef.value.findIndex(f => f.uid === uid)
    if (fIdx >= 0) fileListRef.value.splice(fIdx, 1)
    const iIdx = imagesRef.value.findIndex(img => img.uid === uid)
    if (iIdx >= 0) imagesRef.value.splice(iIdx, 1)
  }
}

const handleCreateImageRemove = makeImageRemover(createImages, createImageList)
const handleEditImageRemove = makeImageRemover(editImages, editImageList)

function handleImageExceed() {
  ElMessage.warning(`最多上传 ${MAX_IMAGE_COUNT} 张现场图片`)
}

// 受控图片 URL → 带 token 的 blob URL（文件接口需鉴权访问）
async function resolveImageSrc(url: string): Promise<string> {
  if (!url) return ''
  // 已是 blob/data URL 直接返回
  if (/^(blob:|data:)/.test(url)) return url
  if (detailImageSrc[url]) return detailImageSrc[url]
  try {
    // url 形如 /api/v1/files/7001/content，使用空 baseURL 走完整路径（命中 vite 代理）
    const res = await api.get(url, { baseURL: '', responseType: 'blob' })
    const blobUrl = URL.createObjectURL(res.data)
    detailImageSrc[url] = blobUrl
    return blobUrl
  } catch (e) {
    console.error('加载图片失败', e)
    return ''
  }
}

// 打开详情时预加载图片 blob URL
async function preloadDetailImages(record: TrainingRecord) {
  // 清理旧的 blob URL
  Object.values(detailImageSrc).forEach(u => {
    if (u.startsWith('blob:')) URL.revokeObjectURL(u)
  })
  Object.keys(detailImageSrc).forEach(k => delete detailImageSrc[k])
  if (!record.images?.length) return
  await Promise.all(record.images.map(img => resolveImageSrc(img.url)))
}

function openDetail(record: TrainingRecord) {
  currentRecord.value = record
  detailVisible.value = true
  preloadDetailImages(record)
}

// 打开新增对话框前重置图片状态
function openCreate() {
  resetForm()
  dialog.value = true
}

// 将后端返回的图片信息映射为 el-upload 的 fileList（用于编辑回显）
function imagesToFileList(images: StagedImage[] = []): UploadUserFile[] {
  return images.map(img => ({
    name: img.fileName,
    uid: img.uid,
    url: img.url,
    status: 'success'
  }))
}

async function openEdit(record: TrainingRecord) {
  if (!auth.isAdmin && record.studentId !== auth.user?.id) {
    ElMessage.warning('您只能编辑自己的实训记录')
    return
  }
  if (record.status === 'APPROVED') {
    ElMessage.warning('教师已评价的记录无法修改')
    return
  }
  editRecord.value = { ...record }
  // 深拷贝图片并补齐 uid（用于删除定位），避免直接修改原始数据
  editImages.value = (record.images || []).map((img, idx) => ({
    fileId: img.fileId,
    fileName: img.fileName,
    url: img.url,
    uid: Number(img.fileId) || Date.now() + idx
  }))
  editImageList.value = imagesToFileList(editImages.value)
  editVisible.value = true
  // 已有图片的缩略图需鉴权访问，转成 blob URL 后回填到 fileList
  await Promise.all(
    editImages.value.map(async (img, idx) => {
      const blobUrl = await resolveImageSrc(img.url)
      if (blobUrl && editImageList.value[idx]) {
        editImageList.value[idx].url = blobUrl
      }
    })
  )
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
      result: editRecord.value.result,
      images: editImages.value.map(img => ({
        fileId: img.fileId,
        fileName: img.fileName,
        url: img.url
      }))
    })
    ElMessage.success('实训记录已更新')
    editVisible.value = false
    await load()
  } catch {
    // api 拦截器已处理错误提示
  }
}

// 组件卸载时释放 blob URL，避免内存泄漏
onBeforeUnmount(() => {
  Object.values(detailImageSrc).forEach(u => {
    if (u.startsWith('blob:')) URL.revokeObjectURL(u)
  })
})

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
        <el-button type="primary" :icon="Plus" @click="openCreate">新增记录</el-button>
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
        <ErrorState
          v-if="error"
          title="加载失败"
          description="实训记录加载出现问题，请检查网络后重试"
          :error="error"
          @retry="load"
        />
        <EmptyState
          v-else-if="records.length === 0"
          title="暂无实训记录"
          description="还没有任何实训记录，点击右上角新增记录开始提交"
        />
        <el-table v-else :data="records" stripe>
          <el-table-column prop="recordDate" label="日期" width="120" />
          <el-table-column label="抽查数据" width="150">
            <template #default="{ row }">
              <strong>{{ row.inspectedTreeCount || 0 }}</strong> 株 / 异常
              <span class="priority-high">{{ row.abnormalTreeCount || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="phenomenon" label="现场现象" min-width="240" show-overflow-tooltip />
          <el-table-column label="图片" width="80" align="center">
            <template #default="{ row }">
              <span v-if="row.images && row.images.length" class="img-count">
                <el-icon><Picture /></el-icon>
                {{ row.images.length }}
              </span>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
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
        <el-form-item label="现场图片">
          <el-upload
            v-model:file-list="createImageList"
            list-type="picture-card"
            accept="image/jpeg,image/png,image/webp"
            :auto-upload="true"
            :http-request="uploadCreateImage"
            :on-remove="handleCreateImageRemove"
            :on-exceed="handleImageExceed"
            :limit="MAX_IMAGE_COUNT"
            multiple
          >
            <el-icon class="upload-add-icon"><Plus /></el-icon>
          </el-upload>
          <div class="image-upload-tip">
            最多 {{ MAX_IMAGE_COUNT }} 张现场照片，单张不超过 5 MB，支持 JPEG / PNG / WebP
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="uploadingImage" @click="submit">提交记录</el-button>
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

        <div class="detail-section" v-if="currentRecord.images && currentRecord.images.length">
          <div class="detail-label">
            <el-icon><Picture /></el-icon> 现场图片
            <span class="img-count-tip">共 {{ currentRecord.images.length }} 张</span>
          </div>
          <div class="image-gallery">
            <el-image
              v-for="(img, idx) in currentRecord.images"
              :key="img.fileId"
              :src="detailImageSrc[img.url]"
              :preview-src-list="currentRecord.images.map(i => detailImageSrc[i.url]).filter(Boolean)"
              :initial-index="idx"
              fit="cover"
              class="gallery-thumb"
              preview-teleported
              hide-on-click-modal
            >
              <template #placeholder>
                <div class="gallery-loading">加载中…</div>
              </template>
              <template #error>
                <div class="gallery-error">
                  <el-icon><Picture /></el-icon>
                  <span>加载失败</span>
                </div>
              </template>
            </el-image>
          </div>
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
        <el-form-item label="现场图片">
          <el-upload
            v-model:file-list="editImageList"
            list-type="picture-card"
            accept="image/jpeg,image/png,image/webp"
            :auto-upload="true"
            :http-request="uploadEditImage"
            :on-remove="handleEditImageRemove"
            :on-exceed="handleImageExceed"
            :limit="MAX_IMAGE_COUNT"
            multiple
          >
            <el-icon class="upload-add-icon"><Plus /></el-icon>
          </el-upload>
          <div class="image-upload-tip">
            最多 {{ MAX_IMAGE_COUNT }} 张现场照片，单张不超过 5 MB，支持 JPEG / PNG / WebP
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploadingImage" @click="saveEdit">保存修改</el-button>
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

/* 表格图片数量指示 */
.img-count {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--green);
  font-weight: 600;
  font-size: 13px;
}

.img-count .el-icon {
  font-size: 14px;
}

/* 图片上传 */
.upload-add-icon {
  font-size: 22px;
  color: var(--muted);
}

.image-upload-tip {
  font-size: 12px;
  color: var(--muted);
  margin-top: 6px;
  line-height: 1.5;
}

/* 详情图片画廊 */
.img-count-tip {
  margin-left: 6px;
  font-weight: 400;
  color: var(--muted);
  font-size: 12px;
}

.image-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(96px, 1fr));
  gap: 8px;
}

.gallery-thumb {
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 6px;
  border: 1px solid var(--line);
  overflow: hidden;
  background: var(--green-light);
  cursor: pointer;
}

.gallery-loading,
.gallery-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  height: 100%;
  color: var(--muted);
  font-size: 11px;
  background: #f7f9f6;
}

.gallery-error .el-icon {
  font-size: 22px;
  color: var(--line);
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