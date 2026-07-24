<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import api, { unwrap } from '@/api'
import type { Orchard, PageData, PhenologyRecord } from '@/types'
import {
  Plus,
  Edit,
  Delete,
  Clock,
  AddLocation,
  Cherry,
  Bowl,
  Users,
  Calendar,
  AlertCircle,
  Search,
  Refresh,
  Check,
  Close
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'

const auth = useAuthStore()

const orchards = ref<Orchard[]>([])
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

const dialog = ref(false)
const dialogType = ref<'create' | 'edit'>('create')
const form = reactive<any>({
  name: '',
  areaMu: 0,
  treeCount: 0,
  treeAgeYears: 0,
  variety: '',
  plantingMode: '露地栽培',
  irrigationMode: '滴灌',
  plantingDate: '',
  province: '',
  city: '',
  district: '',
  longitude: '',
  latitude: '',
  managerName: '',
  remark: ''
})

const phenologyDialog = ref(false)
const phenologyOrchardId = ref('')
const phenology = reactive({
  phenology: 'FRUIT_EClosePANSION',
  effectiveDate: new Date().toISOString().slice(0, 10),
  remark: ''
})

const phenologyHistoryDialog = ref(false)
const historyOrchardId = ref('')
const historyData = ref<PhenologyRecord[]>([])

const phenologyNames: Record<string, string> = {
  DORMANCY: '休眠/恢复期',
  SHOOT_GROWTH: '春梢生长期',
  FLOWERING: '开花期',
  FRUIT_SET: '坐果期',
  FRUIT_EClosePANSION: '幼果膨大期',
  MATURITY: '成熟期',
  HARVEST: '采收期',
  POST_HARVEST: '采后管理期'
}

const statusMap: Record<string, { label: string; class: string }> = {
  ENABLED: { label: '启用', class: 'status-enabled' },
  DISABLED: { label: '停用', class: 'status-disabled' }
}

const filteredOrchards = computed(() => {
  return orchards.value.filter(o => {
    if (search.status && o.status !== search.status) return false
    if (search.keyword) {
      const kw = search.keyword.toLowerCase()
      return (
        o.name.toLowerCase().includes(kw) ||
        o.variety.toLowerCase().includes(kw) ||
        o.region.toLowerCase().includes(kw) ||
        o.managerName.toLowerCase().includes(kw)
      )
    }
    return true
  })
})

async function loadOrchards() {
  loading.value = true
  try {
    const result = unwrap<PageData<Orchard>>(
      await api.get('/orchards', {
        params: {
          page: pagination.page,
          pageSize: pagination.pageSize,
          keyword: search.keyword,
          status: search.status
        }
      })
    )
    orchards.value = result.items
    pagination.total = result.total
  } catch (e) {
    console.error('加载果园列表失败', e)
  } finally {
    loading.value = false
  }
}

async function refresh() {
  pagination.page = 1
  loadOrchards()
}

function openCreateDialog() {
  dialogType.value = 'create'
  Object.assign(form, {
    name: '',
    areaMu: 0,
    treeCount: 0,
    treeAgeYears: 0,
    variety: '',
    plantingMode: '露地栽培',
    irrigationMode: '滴灌',
    plantingDate: '',
    province: '',
    city: '',
    district: '',
    longitude: '',
    latitude: '',
    managerName: '',
    remark: ''
  })
  dialog.value = true
}

function openEditDialog(orchard: Orchard) {
  dialogType.value = 'edit'
  Object.assign(form, {
    id: orchard.id,
    name: orchard.name,
    areaMu: orchard.areaMu,
    treeCount: orchard.treeCount,
    treeAgeYears: orchard.treeAgeYears,
    variety: orchard.variety,
    plantingMode: orchard.plantingMode,
    irrigationMode: orchard.irrigationMode,
    plantingDate: orchard.plantingDate || '',
    province: orchard.province,
    city: orchard.city,
    district: orchard.district,
    longitude: orchard.longitude?.toString() || '',
    latitude: orchard.latitude?.toString() || '',
    managerName: orchard.managerName,
    remark: orchard.remark || ''
  })
  dialog.value = true
}

async function saveOrchard() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入果园名称')
    return
  }
  if (form.areaMu <= 0) {
    ElMessage.warning('面积必须大于0')
    return
  }
  if (form.treeCount <= 0) {
    ElMessage.warning('株数必须大于0')
    return
  }

  const data = {
    ...form,
    longitude: form.longitude ? parseFloat(form.longitude) : undefined,
    latitude: form.latitude ? parseFloat(form.latitude) : undefined,
    region: `${form.province}${form.city}${form.district}`
  }

  try {
    if (dialogType.value === 'create') {
      await api.post('/orchards', data)
      ElMessage.success('果园档案创建成功')
    } else {
      await api.put(`/orchards/${form.id}`, data)
      ElMessage.success('果园档案更新成功')
    }
    dialog.value = false
    loadOrchards()
  } catch (e) {
    console.error('保存果园失败', e)
  }
}

async function toggleStatus(orchard: Orchard) {
  const newStatus = orchard.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const action = newStatus === 'ENABLED' ? '启用' : '停用'

  try {
    await ElMessageBox.confirm(
      `确定要${action}「${orchard.name}」吗？${newStatus === 'DISABLED' ? '停用后将不再生成新任务，但历史数据保留。' : ''}`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await api.patch(`/orchards/${orchard.id}/status`, { status: newStatus })
    ElMessage.success(`${action}成功`)
    loadOrchards()
  } catch {
    ElMessage.info('已取消操作')
  }
}

function openPhenologyDialog(orchard: Orchard) {
  phenologyOrchardId.value = orchard.id
  phenology.phenology = orchard.currentPhenology || 'FRUIT_EClosePANSION'
  phenology.effectiveDate = new Date().toISOString().slice(0, 10)
  phenology.remark = ''
  phenologyDialog.value = true
}

async function savePhenology() {
  if (!phenologyOrchardId.value) return

  try {
    await api.post(`/orchards/${phenologyOrchardId.value}/phenologies`, phenology)
    ElMessage.success('物候期已更新并保留历史')
    phenologyDialog.value = false
    loadOrchards()
  } catch (e) {
    console.error('更新物候期失败', e)
  }
}

async function openHistoryDialog(orchard: Orchard) {
  historyOrchardId.value = orchard.id
  try {
    const result = unwrap<PageData<PhenologyRecord>>(
      await api.get(`/orchards/${orchard.id}/phenologies`, { params: { pageSize: 50 } })
    )
    historyData.value = result.items
    phenologyHistoryDialog.value = true
  } catch (e) {
    console.error('加载物候期历史失败', e)
    ElMessage.error('加载物候期历史失败')
  }
}

function getRegion(orchard: Orchard) {
  return `${orchard.province}${orchard.city}${orchard.district}`
}

onMounted(loadOrchards)
</script>

<template>
  <div class="orchards-page">
    <div class="page-header-row">
      <div>
        <h2>果园档案管理</h2>
        <p>管理果园基础信息与物候期记录</p>
      </div>
      <div v-if="auth.isAdmin" class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增果园</el-button>
      </div>
    </div>

    <div class="search-bar">
      <div class="search-group">
        <el-input
          v-model="search.keyword"
          :prefix-icon="Search"
          placeholder="搜索果园名称、品种、地区、负责人"
          clearable
          @keyup.enter="refresh"
        />
      </div>
      <div class="filter-group">
        <el-select v-model="search.status" placeholder="状态" clearable>
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button :icon="Refresh" @click="refresh">刷新</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <h3>果园列表</h3>
        <span class="count">共 {{ pagination.total }} 个果园</span>
      </div>

      <div class="table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>果园名称</th>
              <th>面积</th>
              <th>株数</th>
              <th>树龄</th>
              <th>品种</th>
              <th>地区</th>
              <th>灌溉方式</th>
              <th>当前物候期</th>
              <th>负责人</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="orchard in orchards" :key="orchard.id">
              <td class="name-cell">
                <Cherry class="icon" />
                <span>{{ orchard.name }}</span>
              </td>
              <td>{{ orchard.areaMu }} 亩</td>
              <td>{{ orchard.treeCount }} 株</td>
              <td>{{ orchard.treeAgeYears }} 年</td>
              <td>{{ orchard.variety }}</td>
              <td class="region-cell">
                <AddLocation class="icon" />
                <span>{{ getRegion(orchard) }}</span>
              </td>
              <td>{{ orchard.irrigationMode }}</td>
              <td class="phenology-cell">
                <span class="phenology-tag">{{ phenologyNames[orchard.currentPhenology] || orchard.currentPhenology }}</span>
              </td>
              <td>{{ orchard.managerName }}</td>
              <td>
                <span :class="['status-badge', statusMap[orchard.status]?.class]">
                  {{ statusMap[orchard.status]?.label }}
                </span>
              </td>
              <td class="actions-cell">
                <div class="actions">
                  <button
                    v-if="auth.isAdmin"
                    class="action-btn edit"
                    title="编辑档案"
                    @click="openEditDialog(orchard)"
                  >
                    <Edit />
                  </button>
                  <button
                    v-if="auth.isAdmin"
                    class="action-btn phenology"
                    title="更新物候期"
                    @click="openPhenologyDialog(orchard)"
                  >
                    <Clock />
                  </button>
                  <button
                    class="action-btn history"
                    title="物候期历史"
                    @click="openHistoryDialog(orchard)"
                  >
                    <Calendar />
                  </button>
                  <button
                    v-if="auth.isAdmin"
                    class="action-btn toggle"
                    :title="orchard.status === 'ENABLED' ? '停用' : '启用'"
                    @click="toggleStatus(orchard)"
                  >
                    <Close v-if="orchard.status === 'ENABLED'" />
                    <Check v-else />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="loading" class="loading-overlay">
          <el-spinner />
        </div>

        <div v-if="!loading && orchards.length === 0" class="empty-state">
          <Cherry class="empty-icon" />
          <p>暂无果园数据</p>
          <p v-if="auth.isAdmin" class="empty-hint">点击上方「新增果园」创建第一个果园档案</p>
        </div>
      </div>

      <div class="pagination-bar" v-if="pagination.total > 0">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 15, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadOrchards"
          @current-change="loadOrchards"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialog"
      :title="dialogType === 'create' ? '新增果园档案' : '编辑果园档案'"
      width="min(720px, 94vw)"
      destroy-on-close
    >
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="果园名称" required>
            <el-input v-model="form.name" placeholder="请输入果园名称" />
          </el-form-item>
          <el-form-item label="品种" required>
            <el-input v-model="form.variety" placeholder="请输入品种名称" />
          </el-form-item>
          <el-form-item label="面积（亩）" required>
            <el-input-number v-model="form.areaMu" :min="0.01" :step="0.1" placeholder="面积" />
          </el-form-item>
          <el-form-item label="株数" required>
            <el-input-number v-model="form.treeCount" :min="1" placeholder="株数" />
          </el-form-item>
          <el-form-item label="树龄（年）">
            <el-input-number v-model="form.treeAgeYears" :min="0" placeholder="树龄" />
          </el-form-item>
          <el-form-item label="种植方式">
            <el-select v-model="form.plantingMode" style="width: 100%">
              <el-option label="露地栽培" value="露地栽培" />
              <el-option label="设施栽培" value="设施栽培" />
              <el-option label="盆栽" value="盆栽" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>
          <el-form-item label="灌溉方式">
            <el-select v-model="form.irrigationMode" style="width: 100%">
              <el-option label="滴灌" value="滴灌" />
              <el-option label="喷灌" value="喷灌" />
              <el-option label="沟灌" value="沟灌" />
              <el-option label="漫灌" value="漫灌" />
              <el-option label="人工浇灌" value="人工浇灌" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>
          <el-form-item label="负责人">
            <el-input v-model="form.managerName" placeholder="请输入负责人姓名" />
          </el-form-item>
          <el-form-item label="省份">
            <el-input v-model="form.province" placeholder="如：福建省" />
          </el-form-item>
          <el-form-item label="城市">
            <el-input v-model="form.city" placeholder="如：福州市" />
          </el-form-item>
          <el-form-item label="区县">
            <el-input v-model="form.district" placeholder="如：闽侯县" />
          </el-form-item>
          <el-form-item label="定植日期">
            <el-date-picker v-model="form.plantingDate" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="经度">
            <el-input v-model="form.longitude" type="number" placeholder="经度" />
          </el-form-item>
          <el-form-item label="纬度">
            <el-input v-model="form.latitude" type="number" placeholder="纬度" />
          </el-form-item>
          <el-form-item label="备注" :span="2">
            <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注信息" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="saveOrchard">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="phenologyDialog" title="更新物候期" width="min(480px, 92vw)" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="物候期" required>
          <el-select v-model="phenology.phenology" style="width: 100%">
            <el-option
              v-for="(label, key) in phenologyNames"
              :key="key"
              :label="label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="生效日期" required>
          <el-date-picker
            v-model="phenology.effectiveDate"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="phenology.remark" type="textarea" :rows="3" placeholder="如：教师现场确认进入幼果膨大期" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="phenologyDialog = false">取消</el-button>
        <el-button type="primary" @click="savePhenology">确认更新</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="phenologyHistoryDialog" title="物候期历史" width="min(560px, 92vw)" destroy-on-close>
      <div v-if="historyData.length > 0">
        <el-timeline class="history-timeline">
          <el-timeline-item
            v-for="(item, index) in historyData"
            :key="item.id"
            :timestamp="item.effectiveDate"
            placement="top"
            :color="index === 0 ? '#2e6b4e' : '#9fbd63'"
          >
            <div class="timeline-content">
              <strong>{{ phenologyNames[item.phenology] || item.phenology }}</strong>
              <p v-if="item.remark">{{ item.remark }}</p>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
      <el-empty v-else description="暂无物候期变更记录" :image-size="80" />
      <template #footer>
        <el-button @click="phenologyHistoryDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.orchards-page {
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
}

.data-table tbody tr:hover {
  background: var(--green-light);
}

.name-cell,
.region-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.name-cell .icon,
.region-cell .icon {
  color: var(--green);
  font-size: 16px;
}

.phenology-cell {
  white-space: nowrap;
}

.phenology-tag {
  display: inline-block;
  padding: 4px 10px;
  background: var(--green-light);
  color: var(--green);
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
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

.action-btn.toggle:hover {
  border-color: var(--amber);
  color: var(--amber);
  background: #fff5e6;
}

.loading-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
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

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 16px 18px;
  border-top: 1px solid var(--line);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.form-grid .el-form-item {
  margin-bottom: 0;
}

.form-grid .el-form-item:nth-child(15) {
  grid-column: span 2;
}

.history-timeline {
  padding: 12px 24px;
}

.timeline-content {
  padding: 10px 14px;
  background: var(--green-light);
  border-radius: 6px;
}

.timeline-content strong {
  font-size: 13px;
  color: var(--green-dark);
}

.timeline-content p {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--muted);
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

  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-grid .el-form-item:nth-child(15) {
    grid-column: span 1;
  }
}
</style>
