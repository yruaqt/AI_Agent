<script setup lang="ts">
import { onMounted, reactive, ref, computed, watch } from 'vue'
import api, { unwrap } from '@/api'
import type { PageData } from '@/types'
import { useAuthStore } from '@/stores/auth'
import {
  Upload,
  Refresh,
  Search,
  Delete,
  View,
  Files,
  Document,
  Link,
  Location,
  Calendar,
  OfficeBuilding,
  WarningFilled,
  CircleCheck,
  Loading,
  CircleClose
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SkeletonTable from '@/components/SkeletonTable.vue'

const auth = useAuthStore()

// 文档数据
interface KnowledgeDocument {
  id: string
  title: string
  sourceOrganization?: string
  publishDate?: string
  region?: string
  phenology?: string
  documentType?: string
  status: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED'
  chunkCount?: number
  fileSize?: number
  fileType?: string
  errorMessage?: string
  createdAt?: string
  updatedAt?: string
}

interface SearchHit {
  documentId: string
  documentName: string
  chunkId?: string
  chunkNo?: number
  page?: number
  content: string
  score: number
  source?: string
}

// 列表与分页
const docs = ref<KnowledgeDocument[]>([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 15,
  total: 0
})
const search = reactive({
  keyword: '',
  status: ''
})

// 统一字典
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

const documentTypeOptions = [
  '教材',
  '标准',
  '农技资料',
  '校本资料',
  '技术规程',
  '科研论文',
  '其他'
]

const statusMap: Record<string, { label: string; class: string; icon: any }> = {
  PENDING: { label: '待处理', class: 'status-pending', icon: Loading },
  PROCESSING: { label: '处理中', class: 'status-processing', icon: Loading },
  SUCCESS: { label: '已就绪', class: 'status-success', icon: CircleCheck },
  FAILED: { label: '处理失败', class: 'status-failed', icon: CircleClose }
}

// 上传对话框
const uploadDialog = ref(false)
const uploadFile = ref<File>()
const uploading = ref(false)
const uploadForm = reactive({
  title: '',
  sourceOrganization: '',
  publishDate: '',
  region: '福建省',
  phenology: '',
  documentType: '农技资料'
})

// 文档详情抽屉
const detailVisible = ref(false)
const currentDoc = ref<KnowledgeDocument | null>(null)
const detailLoading = ref(false)

// 检索测试对话框
const searchDialog = ref(false)
const searching = ref(false)
const searchForm = reactive({
  query: '幼果膨大期大雨前如何管理水肥',
  maxResults: 5,
  minScore: 0.65,
  phenology: '',
  region: ''
})
const searchResults = ref<SearchHit[]>([])

// 统计卡片（基于当前页数据，资料总数使用分页总数）
const stats = computed(() => {
  const total = pagination.total
  const ready = docs.value.filter(d => d.status === 'SUCCESS').length
  const processing = docs.value.filter(d => d.status === 'PENDING' || d.status === 'PROCESSING').length
  const failed = docs.value.filter(d => d.status === 'FAILED').length
  const chunks = docs.value.reduce((sum, d) => sum + (d.chunkCount || 0), 0)
  return { total, ready, processing, failed, chunks }
})

async function load() {
  loading.value = true
  try {
    const result = unwrap<PageData<KnowledgeDocument>>(
      await api.get('/knowledge/documents', {
        params: {
          page: pagination.page,
          pageSize: pagination.pageSize,
          keyword: search.keyword || undefined,
          status: search.status || undefined
        }
      })
    )
    docs.value = result.items
    pagination.total = result.total
  } catch (e) {
    console.error('加载文档列表失败', e)
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

function resetSearch() {
  search.keyword = ''
  search.status = ''
  refresh()
}

// 上传相关
function openUploadDialog() {
  uploadFile.value = undefined
  Object.assign(uploadForm, {
    title: '',
    sourceOrganization: '',
    publishDate: '',
    region: '福建省',
    phenology: '',
    documentType: '农技资料'
  })
  uploadDialog.value = true
}

function selectFile(raw: any) {
  uploadFile.value = raw.raw
  // 若未填名称，自动使用文件名
  if (!uploadForm.title && raw.name) {
    uploadForm.title = raw.name.replace(/\.[^.]+$/, '')
  }
}

function handleFileRemove() {
  uploadFile.value = undefined
}

async function upload() {
  if (!uploadFile.value) {
    ElMessage.warning('请选择要上传的文档')
    return
  }
  if (!uploadForm.title.trim()) {
    ElMessage.warning('请输入资料名称')
    return
  }
  if (!uploadForm.sourceOrganization.trim()) {
    ElMessage.warning('请输入来源单位')
    return
  }
  if (!uploadForm.documentType) {
    ElMessage.warning('请选择资料类型')
    return
  }

  // 大小限制 20MB
  if (uploadFile.value.size > 20 * 1024 * 1024) {
    ElMessage.warning('单个文件不能超过 20 MB')
    return
  }

  const fd = new FormData()
  fd.append('file', uploadFile.value)
  fd.append('title', uploadForm.title.trim())
  fd.append('sourceOrganization', uploadForm.sourceOrganization.trim())
  fd.append('documentType', uploadForm.documentType)
  if (uploadForm.publishDate) fd.append('publishDate', uploadForm.publishDate)
  if (uploadForm.region) fd.append('region', uploadForm.region)
  if (uploadForm.phenology) fd.append('phenology', uploadForm.phenology)

  uploading.value = true
  try {
    await api.post('/knowledge/documents', fd)
    ElMessage.success('文档已接收，正在后台解析与向量化')
    uploadDialog.value = false
    refresh()
  } catch (e) {
    console.error('上传文档失败', e)
  } finally {
    uploading.value = false
  }
}

// 重新处理
async function reindex(row: KnowledgeDocument) {
  try {
    await ElMessageBox.confirm(
      `确定要重新处理「${row.title}」吗？该操作将重新解析与向量化。`,
      '确认重新处理',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
    await api.post(`/knowledge/documents/${row.id}/reindex`)
    ElMessage.success('已提交重新处理，请稍后刷新查看状态')
    load()
  } catch (action) {
    if (action !== 'cancel') {
      ElMessage.error('重新处理失败')
    }
  }
}

// 删除文档
async function remove(row: KnowledgeDocument) {
  try {
    await ElMessageBox.confirm(
      `删除文档「${row.title}」将同步删除其所有检索片段，且不可恢复。是否继续？`,
      '确认删除',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await api.delete(`/knowledge/documents/${row.id}`)
    ElMessage.success('文档已删除')
    load()
  } catch (action) {
    if (action !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 文档详情
async function openDetail(row: KnowledgeDocument) {
  currentDoc.value = { ...row }
  detailVisible.value = true
  detailLoading.value = true
  try {
    const detail = unwrap<KnowledgeDocument>(
      await api.get(`/knowledge/documents/${row.id}`)
    )
    currentDoc.value = detail
  } catch (e) {
    console.error('加载文档详情失败', e)
  } finally {
    detailLoading.value = false
  }
}

// 检索测试
function openSearchDialog() {
  searchDialog.value = true
  searchResults.value = []
}

async function runSearch() {
  if (!searchForm.query.trim()) {
    ElMessage.warning('请输入检索问题')
    return
  }
  searching.value = true
  try {
    const payload: any = {
      query: searchForm.query.trim(),
      maxResults: searchForm.maxResults,
      minScore: searchForm.minScore
    }
    const filters: any = {}
    if (searchForm.phenology) filters.phenology = searchForm.phenology
    if (searchForm.region) filters.region = searchForm.region
    if (Object.keys(filters).length > 0) payload.filters = filters

    searchResults.value = unwrap<SearchHit[]>(
      await api.post('/knowledge/search-test', payload)
    )
    if (searchResults.value.length === 0) {
      ElMessage.info('未找到可靠知识来源')
    }
  } catch (e) {
    console.error('检索失败', e)
  } finally {
    searching.value = false
  }
}

function formatScore(score: number) {
  return (score * 100).toFixed(1) + '%'
}

function formatSize(bytes?: number) {
  if (!bytes) return '—'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

onMounted(load)
</script>

<template>
  <div class="knowledge-page">
    <div class="page-header-row">
      <div>
        <h2>专业知识库</h2>
        <p>橄榄种植资料的解析、向量化与来源追踪</p>
      </div>
      <div class="toolbar">
        <el-button :icon="Search" @click="openSearchDialog">检索测试</el-button>
        <el-button v-if="auth.isAdmin" type="primary" :icon="Upload" @click="openUploadDialog">上传资料</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card">
        <span class="label">资料总数</span>
        <strong>{{ stats.total }}</strong>
        <span class="trend">{{ stats.chunks }} 个检索片段</span>
      </div>
      <div class="stat-card">
        <span class="label">已就绪</span>
        <strong class="text-green">{{ stats.ready }}</strong>
        <span class="trend">可参与问答</span>
      </div>
      <div class="stat-card">
        <span class="label">处理中</span>
        <strong class="text-amber">{{ stats.processing }}</strong>
        <span class="trend">解析 / 向量化</span>
      </div>
      <div class="stat-card">
        <span class="label">处理失败</span>
        <strong class="text-red">{{ stats.failed }}</strong>
        <span class="trend">需要重新处理</span>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-group">
        <el-input
          v-model="search.keyword"
          :prefix-icon="Search"
          placeholder="搜索资料名称、来源单位"
          clearable
          @keyup.enter="refresh"
        />
      </div>
      <div class="filter-group">
        <el-select v-model="search.status" placeholder="处理状态" clearable style="width: 140px;">
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已就绪" value="SUCCESS" />
          <el-option label="处理失败" value="FAILED" />
        </el-select>
        <el-button :icon="Refresh" @click="refresh">刷新</el-button>
        <el-button link @click="resetSearch">重置</el-button>
      </div>
    </div>

    <!-- 文档列表 -->
    <div class="panel">
      <div class="panel-header">
        <h3>资料列表</h3>
        <span class="count">共 {{ pagination.total }} 份资料</span>
      </div>

      <div class="table-wrapper">
        <template v-if="loading">
          <SkeletonTable :rows="6" :columns="9" />
        </template>
        <template v-else>
          <table class="data-table">
            <thead>
              <tr>
                <th>资料名称</th>
                <th>来源单位</th>
                <th>类型</th>
                <th>地区</th>
                <th>物候期</th>
                <th>片段</th>
                <th>状态</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="doc in docs" :key="doc.id">
                <td class="name-cell">
                  <Document class="icon" />
                  <div class="name-info">
                    <strong>{{ doc.title }}</strong>
                    <span v-if="doc.fileType" class="file-type">{{ doc.fileType }}</span>
                  </div>
                </td>
                <td class="source-cell">
                  <OfficeBuilding class="icon-sm" />
                  <span>{{ doc.sourceOrganization || '—' }}</span>
                </td>
                <td>
                  <span v-if="doc.documentType" class="type-tag">{{ doc.documentType }}</span>
                  <span v-else class="muted">—</span>
                </td>
                <td class="region-cell">
                  <Location v-if="doc.region" class="icon-sm" />
                  <span>{{ doc.region || '—' }}</span>
                </td>
                <td>
                  <span v-if="doc.phenology" class="phenology-tag">
                    {{ phenologyNames[doc.phenology] || doc.phenology }}
                  </span>
                  <span v-else class="muted">—</span>
                </td>
                <td>
                  <span v-if="doc.chunkCount !== undefined" class="chunk-count">{{ doc.chunkCount }}</span>
                  <span v-else class="muted">—</span>
                </td>
                <td>
                  <span :class="['status-badge', statusMap[doc.status]?.class]">
                    <el-icon v-if="statusMap[doc.status]" class="status-icon">
                      <component :is="statusMap[doc.status].icon" />
                    </el-icon>
                    {{ statusMap[doc.status]?.label || doc.status }}
                  </span>
                </td>
                <td class="time-cell">{{ doc.updatedAt || doc.createdAt || '—' }}</td>
                <td class="actions-cell">
                  <div class="actions">
                    <button class="action-btn view" title="查看详情" @click="openDetail(doc)">
                      <View />
                    </button>
                    <button
                      v-if="auth.isAdmin"
                      class="action-btn reindex"
                      title="重新处理"
                      @click="reindex(doc)"
                    >
                      <Refresh />
                    </button>
                    <button
                      v-if="auth.isAdmin"
                      class="action-btn delete"
                      title="删除"
                      @click="remove(doc)"
                    >
                      <Delete />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <div v-if="docs.length === 0" class="empty-state">
            <Files class="empty-icon" />
            <p>暂无知识资料</p>
            <p v-if="auth.isAdmin" class="empty-hint">点击上方「上传资料」导入橄榄种植相关文档</p>
          </div>
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

    <!-- 上传对话框 -->
    <el-dialog
      v-model="uploadDialog"
      title="上传知识资料"
      width="min(620px, 94vw)"
      destroy-on-close
    >
      <el-form label-position="top">
        <el-form-item label="文档文件" required>
          <el-upload
            drag
            :auto-upload="false"
            :limit="1"
            accept=".pdf,.docx,.txt,.md,.markdown"
            :on-change="selectFile"
            :on-remove="handleFileRemove"
          >
            <el-icon class="upload-icon"><Upload /></el-icon>
            <div class="upload-text">选择或拖入文件</div>
            <template #tip>
              <div class="upload-tip">支持 PDF、DOCX、TXT、Markdown，单个文件不超过 20 MB</div>
            </template>
          </el-upload>
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="资料名称" required>
            <el-input v-model="uploadForm.title" placeholder="留空将使用文件名" />
          </el-form-item>
          <el-form-item label="来源单位" required>
            <el-input v-model="uploadForm.sourceOrganization" placeholder="如：福建农林大学" />
          </el-form-item>
          <el-form-item label="资料类型" required>
            <el-select v-model="uploadForm.documentType" style="width: 100%">
              <el-option
                v-for="t in documentTypeOptions"
                :key="t"
                :label="t"
                :value="t"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="发布日期">
            <el-date-picker
              v-model="uploadForm.publishDate"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              placeholder="选择发布日期"
            />
          </el-form-item>
          <el-form-item label="适用地区">
            <el-input v-model="uploadForm.region" placeholder="如：福建省" />
          </el-form-item>
          <el-form-item label="适用物候期">
            <el-select v-model="uploadForm.phenology" placeholder="不限" clearable style="width: 100%">
              <el-option
                v-for="(label, key) in phenologyNames"
                :key="key"
                :label="label"
                :value="key"
              />
            </el-select>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="upload">上传并索引</el-button>
      </template>
    </el-dialog>

    <!-- 文档详情抽屉 -->
    <el-drawer
      v-model="detailVisible"
      title="文档详情"
      size="520px"
      destroy-on-close
    >
      <div v-loading="detailLoading" class="doc-detail">
        <div v-if="currentDoc" class="detail-content">
          <div class="detail-title-row">
            <div class="detail-title">
              <Document class="icon" />
              <strong>{{ currentDoc.title }}</strong>
            </div>
            <span :class="['status-badge', statusMap[currentDoc.status]?.class]">
              <el-icon v-if="statusMap[currentDoc.status]" class="status-icon">
                <component :is="statusMap[currentDoc.status].icon" />
              </el-icon>
              {{ statusMap[currentDoc.status]?.label || currentDoc.status }}
            </span>
          </div>

          <div v-if="currentDoc.status === 'FAILED' && currentDoc.errorMessage" class="error-block">
            <el-icon><WarningFilled /></el-icon>
            <div>
              <strong>处理失败原因</strong>
              <p>{{ currentDoc.errorMessage }}</p>
            </div>
          </div>

          <div class="detail-section">
            <div class="detail-label"><OfficeBuilding /> 来源单位</div>
            <div class="detail-value">{{ currentDoc.sourceOrganization || '—' }}</div>
          </div>

          <div class="detail-grid">
            <div class="detail-section">
              <div class="detail-label"><Link /> 资料类型</div>
              <div class="detail-value">{{ currentDoc.documentType || '—' }}</div>
            </div>
            <div class="detail-section">
              <div class="detail-label"><Location /> 适用地区</div>
              <div class="detail-value">{{ currentDoc.region || '—' }}</div>
            </div>
            <div class="detail-section">
              <div class="detail-label"><Calendar /> 适用物候期</div>
              <div class="detail-value">
                {{ currentDoc.phenology ? (phenologyNames[currentDoc.phenology] || currentDoc.phenology) : '—' }}
              </div>
            </div>
            <div class="detail-section">
              <div class="detail-label"><Calendar /> 发布日期</div>
              <div class="detail-value">{{ currentDoc.publishDate || '—' }}</div>
            </div>
          </div>

          <div class="detail-grid">
            <div class="detail-section">
              <div class="detail-label">片段数量</div>
              <div class="detail-value strong">{{ currentDoc.chunkCount ?? '—' }}</div>
            </div>
            <div class="detail-section">
              <div class="detail-label">文件大小</div>
              <div class="detail-value">{{ formatSize(currentDoc.fileSize) }}</div>
            </div>
            <div class="detail-section">
              <div class="detail-label">创建时间</div>
              <div class="detail-value">{{ currentDoc.createdAt || '—' }}</div>
            </div>
            <div class="detail-section">
              <div class="detail-label">更新时间</div>
              <div class="detail-value">{{ currentDoc.updatedAt || '—' }}</div>
            </div>
          </div>

          <div v-if="auth.isAdmin" class="detail-actions">
            <el-button :icon="Refresh" @click="reindex(currentDoc); detailVisible = false">重新处理</el-button>
            <el-button type="danger" :icon="Delete" @click="remove(currentDoc); detailVisible = false">删除文档</el-button>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 检索测试对话框 -->
    <el-dialog
      v-model="searchDialog"
      title="向量检索测试"
      width="min(780px, 94vw)"
      destroy-on-close
    >
      <el-form label-position="top">
        <el-form-item label="检索问题">
          <el-input
            v-model="searchForm.query"
            type="textarea"
            :rows="2"
            placeholder="例如：橄榄幼果期水肥管理"
          />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="返回条数">
            <el-input-number v-model="searchForm.maxResults" :min="1" :max="20" />
          </el-form-item>
          <el-form-item label="最低相似度">
            <el-input-number v-model="searchForm.minScore" :min="0" :max="1" :step="0.05" :precision="2" />
          </el-form-item>
          <el-form-item label="物候期过滤">
            <el-select v-model="searchForm.phenology" placeholder="不限" clearable style="width: 100%">
              <el-option
                v-for="(label, key) in phenologyNames"
                :key="key"
                :label="label"
                :value="key"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="地区过滤">
            <el-input v-model="searchForm.region" placeholder="如：福建省" />
          </el-form-item>
        </div>

        <div class="search-actions">
          <el-button type="primary" :loading="searching" :icon="Search" @click="runSearch">执行检索</el-button>
        </div>
      </el-form>

      <div class="search-results">
        <div v-if="!searchResults.length && !searching" class="muted-empty">
          输入问题并点击「执行检索」查看实际向量检索效果
        </div>

        <div v-if="searchResults.length === 0 && searching" class="muted-empty">
          正在检索...
        </div>

        <div v-if="searchResults.length === 0 && !searching && searchForm.query" class="muted-empty warning">
          <el-icon><WarningFilled /></el-icon>
          未找到可靠知识来源
        </div>

        <div
          v-for="(r, idx) in searchResults"
          :key="r.documentId + '-' + (r.chunkId || r.chunkNo || idx)"
          class="search-hit"
        >
          <header>
            <div class="hit-title">
              <strong>{{ r.documentName }}</strong>
              <span v-if="r.chunkNo !== undefined" class="hit-chunk">片段 {{ r.chunkNo }}</span>
              <span v-if="r.page !== undefined" class="hit-page">第 {{ r.page }} 页</span>
            </div>
            <span class="hit-score">{{ formatScore(r.score) }}</span>
          </header>
          <p>{{ r.content }}</p>
          <small v-if="r.source">{{ r.source }}</small>
        </div>
      </div>

      <template #footer>
        <el-button @click="searchDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.knowledge-page {
  width: 100%;
}

.page-header-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 22px;
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
  flex-wrap: wrap;
}

/* 统计卡片 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 16px 18px;
  min-height: 92px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.stat-card .label {
  color: var(--muted);
  font-size: 12px;
}

.stat-card strong {
  font-size: 26px;
  line-height: 1;
  color: var(--ink);
}

.stat-card .text-green { color: var(--green); }
.stat-card .text-amber { color: var(--amber); }
.stat-card .text-red { color: var(--red); }

.stat-card .trend {
  font-size: 11px;
  color: var(--muted);
}

/* 搜索栏 */
.search-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding: 14px 16px;
  background: white;
  border: 1px solid var(--line);
  border-radius: 6px;
}

.search-group {
  flex: 1;
  max-width: 480px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 面板与表格 */
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
  border-collapse: collapse;
  font-size: 13px;
}

.data-table th {
  background: var(--green-light);
  padding: 14px 16px;
  text-align: left;
  font-weight: 600;
  color: var(--green-dark);
  white-space: nowrap;
  border-bottom: 2px solid var(--line);
}

.data-table td {
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
  color: var(--ink);
  vertical-align: middle;
}

.data-table tbody tr:hover {
  background: var(--green-light);
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 220px;
}

.name-cell .icon {
  color: var(--green);
  font-size: 18px;
  flex: none;
}

.name-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.name-info strong {
  font-size: 13.5px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 280px;
}

.file-type {
  font-size: 11px;
  color: var(--muted);
}

.source-cell,
.region-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.icon-sm {
  font-size: 13px;
  color: var(--muted);
}

.type-tag {
  display: inline-block;
  padding: 3px 10px;
  background: var(--lime-light);
  color: #6f8a3a;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.phenology-tag {
  display: inline-block;
  padding: 3px 10px;
  background: var(--green-light);
  color: var(--green);
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.chunk-count {
  font-weight: 700;
  color: var(--green-dark);
}

.time-cell {
  color: var(--muted);
  font-size: 12px;
  white-space: nowrap;
}

.muted {
  color: var(--muted);
}

/* 状态徽章 */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.status-badge .status-icon {
  font-size: 13px;
}

.status-pending {
  background: #f4f1ea;
  color: #8a7344;
}

.status-processing {
  background: #fff5e6;
  color: var(--amber);
}

.status-success {
  background: #edf4ef;
  color: var(--green);
}

.status-failed {
  background: #f8f0ef;
  color: var(--red);
}

/* 操作按钮 */
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
  width: 32px;
  height: 32px;
  border: 1px solid var(--line);
  border-radius: 5px;
  background: white;
  color: var(--muted);
  transition: all 0.2s;
}

.action-btn:hover {
  border-color: var(--green);
  color: var(--green);
  background: var(--green-light);
}

.action-btn.reindex:hover {
  border-color: var(--amber);
  color: var(--amber);
  background: #fff5e6;
}

.action-btn.delete:hover {
  border-color: var(--red);
  color: var(--red);
  background: #f8f0ef;
}

/* 加载与空状态 */
.loading-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.85);
  color: var(--muted);
  font-size: 13px;
}

.loading-icon {
  font-size: 24px;
  color: var(--green);
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

.empty-state p {
  margin: 0 0 8px;
  font-size: 14px;
}

.empty-hint {
  font-size: 12px !important;
}

/* 分页 */
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 16px 18px;
  border-top: 1px solid var(--line);
}

/* 表单 */
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.form-grid .el-form-item {
  margin-bottom: 0;
}

/* 上传组件 */
.upload-icon {
  font-size: 30px;
  color: var(--green);
  margin-bottom: 8px;
}

.upload-text {
  font-size: 13px;
  color: var(--ink);
}

.upload-tip {
  font-size: 12px;
  color: var(--muted);
  margin-top: 6px;
}

/* 详情抽屉 */
.doc-detail {
  padding: 4px 8px;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--line);
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.detail-title .icon {
  color: var(--green);
  font-size: 22px;
  flex: none;
}

.detail-title strong {
  font-size: 16px;
  line-height: 1.4;
  word-break: break-word;
}

.error-block {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  background: #f8f0ef;
  border: 1px solid #ecc7c4;
  border-radius: 6px;
  color: var(--red);
}

.error-block .el-icon {
  font-size: 18px;
  flex: none;
  margin-top: 2px;
}

.error-block strong {
  display: block;
  font-size: 13px;
  margin-bottom: 4px;
}

.error-block p {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
}

.detail-section {
  margin-bottom: 4px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.detail-label {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.detail-label :deep(.el-icon) {
  font-size: 13px;
}

.detail-value {
  font-size: 14px;
  color: var(--ink);
  word-break: break-word;
}

.detail-value.strong {
  font-weight: 700;
  color: var(--green-dark);
  font-size: 18px;
}

.detail-actions {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
}

/* 检索测试 */
.search-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.search-results {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
}

.muted-empty {
  text-align: center;
  padding: 24px 12px;
  color: var(--muted);
  font-size: 13px;
}

.muted-empty.warning {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--amber);
}

.muted-empty .el-icon {
  font-size: 16px;
}

.search-hit {
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: 6px;
  margin-bottom: 10px;
  background: #fafbf9;
  transition: border-color 0.2s;
}

.search-hit:hover {
  border-color: var(--lime);
}

.search-hit header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
}

.hit-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.hit-title strong {
  font-size: 13px;
  color: var(--ink);
}

.hit-chunk,
.hit-page {
  display: inline-block;
  padding: 2px 8px;
  background: var(--green-light);
  color: var(--green);
  border-radius: 3px;
  font-size: 11px;
}

.hit-score {
  color: var(--green);
  font-weight: 700;
  font-size: 13px;
}

.search-hit p {
  font-size: 12.5px;
  line-height: 1.7;
  color: #4f5b54;
  margin: 0 0 6px;
}

.search-hit small {
  color: var(--muted);
  font-size: 11px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
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

  .stat-grid {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  .stat-card {
    padding: 14px;
    min-height: 84px;
  }

  .stat-card strong {
    font-size: 20px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .filter-group {
    flex-wrap: wrap;
  }

  .name-info strong {
    max-width: 180px;
  }
}
</style>
