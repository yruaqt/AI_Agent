<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import api, { unwrap } from '@/api'
import type { Orchard, PageData, WeatherData, Task, PhenologyRecord } from '@/types'
import { Edit, Clock, Cloudy, Document, Warning } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const auth = useAuthStore()
const orchard = ref<Orchard | null>(null)
const history = ref<PhenologyRecord[]>([])
const weather = ref<WeatherData | null>(null)
const tasks = ref<Task[]>([])
const loading = reactive({ weather: false, tasks: false })
const dialog = ref(false)
const phenologyDialog = ref(false)
const form = reactive<any>({})
const phenology = reactive({
  phenology: 'FRUIT_EXPANSION',
  effectiveDate: new Date().toISOString().slice(0, 10),
  remark: ''
})

// 物候期名称映射
const names: Record<string, string> = {
  DORMANCY: '休眠/恢复期',
  SHOOT_GROWTH: '春梢生长期',
  FLOWERING: '开花期',
  FRUIT_SET: '坐果期',
  FRUIT_EXPANSION: '幼果膨大期',
  MATURITY: '成熟期',
  HARVEST: '采收期',
  POST_HARVEST: '采后管理期'
}

// 任务优先级映射
const priorityMap: Record<string, { label: string; class: string }> = {
  HIGH: { label: '高', class: 'priority-high' },
  MEDIUM: { label: '中', class: 'priority-medium' },
  LOW: { label: '低', class: 'priority-low' }
}

// 任务状态映射
const taskStatusMap: Record<string, string> = {
  DRAFT: '草稿',
  CONFIRMED: '已确认',
  TODO: '待执行',
  DOING: '执行中',
  DONE: '已完成',
  CANCELLED: '已取消'
}

// 加载果园基本信息
async function load() {
  const d = unwrap<PageData<Orchard>>(await api.get('/orchards'))
  orchard.value = d.items[0]
  if (orchard.value) {
    Object.assign(form, orchard.value)
    history.value = unwrap<PageData<PhenologyRecord>>(
      await api.get(`/orchards/${orchard.value.id}/phenologies`)
    ).items
    loadWeather()
    loadTasks()
  }
}

// 加载天气信息
async function loadWeather() {
  if (!orchard.value) return
  loading.weather = true
  try {
    weather.value = unwrap<WeatherData>(
      await api.get(`/orchards/${orchard.value.id}/weather?days=3`)
    )
  } catch (e) {
    console.error('加载天气失败', e)
  } finally {
    loading.weather = false
  }
}

// 加载今日任务
async function loadTasks() {
  if (!orchard.value) return
  loading.tasks = true
  try {
    const today = new Date().toISOString().slice(0, 10)
    const result = unwrap<PageData<Task>>(
      await api.get(`/tasks?orchardId=${orchard.value.id}&date=${today}&pageSize=10`)
    )
    tasks.value = result.items.filter(t => t.status !== 'CANCELLED' && t.status !== 'DONE')
  } catch (e) {
    console.error('加载任务失败', e)
  } finally {
    loading.tasks = false
  }
}

// 保存果园档案
async function save() {
  if (!orchard.value) return
  await api.put(`/orchards/${orchard.value.id}`, form)
  ElMessage.success('果园档案已保存')
  dialog.value = false
  load()
}

// 保存物候期
async function savePhenology() {
  if (!orchard.value) return
  await api.post(`/orchards/${orchard.value.id}/phenologies`, phenology)
  ElMessage.success('物候期已更新并保留历史')
  phenologyDialog.value = false
  load()
}

// 格式化时间
function formatTime(dateStr: string) {
  const date = new Date(dateStr)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 获取天气图标
function getWeatherIcon(weather: string): string {
  if (weather.includes('雨')) return '🌧️'
  if (weather.includes('雪')) return '🌨️'
  if (weather.includes('云')) return '⛅'
  if (weather.includes('晴')) return '☀️'
  if (weather.includes('阴')) return '☁️'
  return '🌤️'
}

onMounted(load)
</script>

<template>
  <div v-if="orchard" class="orchard-overview">
    <!-- 页面标题行 -->
    <div class="page-title-row">
      <div>
        <h2>{{ orchard.name }}</h2>
        <p>{{ orchard.region }} · {{ orchard.variety }}</p>
      </div>
      <div v-if="auth.isAdmin" class="toolbar">
        <el-button :icon="Clock" @click="phenologyDialog = true">更新物候期</el-button>
        <el-button type="primary" :icon="Edit" @click="dialog = true">编辑档案</el-button>
      </div>
    </div>

    <!-- 果园概况带 -->
    <div class="orchard-band">
      <div>
        <span>面积</span>
        <strong>{{ orchard.areaMu }} 亩</strong>
      </div>
      <div>
        <span>株数</span>
        <strong>{{ orchard.treeCount }} 株</strong>
      </div>
      <div>
        <span>树龄</span>
        <strong>{{ orchard.treeAgeYears }} 年</strong>
      </div>
      <div>
        <span>灌溉</span>
        <strong>{{ orchard.irrigationMode }}</strong>
      </div>
      <div>
        <span>物候期</span>
        <strong>{{ names[orchard.currentPhenology] }}</strong>
      </div>
    </div>

    <!-- 统计卡片区域 -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="label">当前温度</div>
        <strong v-if="weather">{{ weather.current.temperatureC }}°C</strong>
        <span v-else class="muted">加载中...</span>
        <div class="trend" v-if="weather">
          {{ weather.current.weather }}
        </div>
      </div>
      <div class="stat-card">
        <div class="label">风力风向</div>
        <strong v-if="weather">{{ weather.current.windLevel }}级</strong>
        <span v-else class="muted">加载中...</span>
        <div class="trend" v-if="weather">
          {{ weather.current.windDirection }}风
        </div>
      </div>
      <div class="stat-card">
        <div class="label">今日待办</div>
        <strong>{{ tasks.length }}</strong>
        <div class="trend">
          项农事任务
        </div>
      </div>
      <div class="stat-card">
        <div class="label">数据更新</div>
        <strong v-if="weather">{{ formatTime(weather.updatedAt) }}</strong>
        <span v-else class="muted">--</span>
        <div class="trend" v-if="weather && weather.cached">
          缓存数据
        </div>
      </div>
    </div>

    <!-- 内容网格 -->
    <div class="content-grid">
      <!-- 左侧主要内容 -->
      <div class="main-columns">
        <!-- 天气预报 -->
        <section class="panel">
          <header class="panel-header">
            <h3><el-icon><Cloudy /></el-icon> 未来天气</h3>
          </header>
          <div class="weather-grid" v-if="weather && weather.forecast.length > 0">
            <div class="weather-item" v-for="f in weather.forecast" :key="f.date">
              <div class="weather-date">{{ f.date.slice(5) }}</div>
              <div class="weather-icon">{{ getWeatherIcon(f.dayWeather) }}</div>
              <div class="weather-desc">{{ f.dayWeather }}</div>
              <div class="weather-temp">
                {{ f.minTemperatureC }}°~{{ f.maxTemperatureC }}°C
              </div>
            </div>
          </div>
          <el-empty v-else-if="!loading.weather" description="暂无天气数据" :image-size="80" />
          <div v-else class="loading-placeholder">加载天气数据...</div>
        </section>

        <!-- 今日任务 -->
        <section class="panel">
          <header class="panel-header">
            <h3><el-icon><Document /></el-icon> 今日农事任务</h3>
          </header>
          <div class="task-list" v-if="tasks.length > 0">
            <div class="task-item" v-for="task in tasks" :key="task.id">
              <div class="task-header">
                <span :class="['priority', priorityMap[task.priority]?.class]">
                  {{ priorityMap[task.priority]?.label }}
                </span>
                <span class="task-status">{{ taskStatusMap[task.status] }}</span>
              </div>
              <h4>{{ task.title }}</h4>
              <p>{{ task.content }}</p>
              <div class="task-footer">
                <span class="task-time">建议时间：{{ task.suggestedTime }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else-if="!loading.tasks" description="今日暂无待办任务" :image-size="80" />
          <div v-else class="loading-placeholder">加载任务数据...</div>
        </section>
      </div>

      <!-- 右侧栏 -->
      <div class="side-columns">
        <!-- 基本档案 -->
        <section class="panel">
          <header class="panel-header">
            <h3>基本档案</h3>
            <span class="status-pill">{{ orchard.status }}</span>
          </header>
          <dl class="detail-list">
            <div>
              <dt>品种</dt>
              <dd>{{ orchard.variety }}</dd>
            </div>
            <div>
              <dt>负责人</dt>
              <dd>{{ orchard.managerName }}</dd>
            </div>
            <div>
              <dt>所在地区</dt>
              <dd>{{ orchard.region }}</dd>
            </div>
            <div>
              <dt>种植方式</dt>
              <dd>露地栽培</dd>
            </div>
            <div>
              <dt>灌溉方式</dt>
              <dd>{{ orchard.irrigationMode }}</dd>
            </div>
          </dl>
        </section>

        <!-- 物候期历史 -->
        <section class="panel">
          <header class="panel-header">
            <h3>物候期历史</h3>
          </header>
          <el-timeline class="timeline">
            <el-timeline-item
              v-for="h in history"
              :key="h.id"
              :timestamp="h.effectiveDate"
              placement="top"
              color="#2e6b4e"
            >
              <strong>{{ names[h.phenology] }}</strong>
              <p>{{ h.remark }}</p>
            </el-timeline-item>
          </el-timeline>
        </section>

        <!-- 风险提示 -->
        <section class="panel risk-panel">
          <header class="panel-header">
            <h3><el-icon><Warning /></el-icon> 风险提示</h3>
          </header>
          <div class="risk-content">
            <p v-if="weather && weather.forecast.some(f => f.dayWeather.includes('雨'))">
              <strong>降雨提醒：</strong>未来有降雨天气，请注意果园排水，避免积水影响果树生长。
            </p>
            <p v-if="tasks.some(t => t.priority === 'HIGH')">
              <strong>高优先级任务：</strong>有高优先级任务待执行，请及时安排人员处理。
            </p>
            <p v-if="weather && weather.current.temperatureC > 35">
              <strong>高温预警：</strong>当前温度较高，请注意防暑降温，避免高温时段作业。
            </p>
            <p v-if="weather && weather.current.temperatureC < 5">
              <strong>低温预警：</strong>当前温度较低，请注意防寒防冻措施。
            </p>
            <p v-if="!weather || (tasks.length === 0 && weather.current.temperatureC >= 5 && weather.current.temperatureC <= 35)">
              <strong>当前状况良好：</strong>暂无明显风险提示，请保持日常管理。
            </p>
          </div>
        </section>
      </div>
    </div>

    <!-- 编辑档案对话框 -->
    <el-dialog v-model="dialog" title="编辑果园档案" width="min(680px, 94vw)">
      <el-form label-position="top">
        <div class="edit-grid">
          <el-form-item label="果园名称">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="所在地区">
            <el-input v-model="form.region" />
          </el-form-item>
          <el-form-item label="面积（亩）">
            <el-input-number v-model="form.areaMu" :min="0.01" />
          </el-form-item>
          <el-form-item label="株数">
            <el-input-number v-model="form.treeCount" :min="1" />
          </el-form-item>
          <el-form-item label="树龄">
            <el-input-number v-model="form.treeAgeYears" :min="0" />
          </el-form-item>
          <el-form-item label="品种">
            <el-input v-model="form.variety" />
          </el-form-item>
          <el-form-item label="灌溉方式">
            <el-input v-model="form.irrigationMode" />
          </el-form-item>
          <el-form-item label="负责人">
            <el-input v-model="form.managerName" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 更新物候期对话框 -->
    <el-dialog v-model="phenologyDialog" title="更新物候期" width="min(460px, 92vw)">
      <el-form label-position="top">
        <el-form-item label="物候期">
          <el-select v-model="phenology.phenology" style="width: 100%">
            <el-option
              v-for="(label, key) in names"
              :key="key"
              :label="label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="生效日期">
          <el-date-picker
            v-model="phenology.effectiveDate"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="phenology.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="phenologyDialog = false">取消</el-button>
        <el-button type="primary" @click="savePhenology">确认更新</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.orchard-overview {
  width: 100%;
}

.orchard-band {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  background: #24352d;
  color: white;
  border-radius: 6px;
  padding: 20px 8px;
  margin-bottom: 18px;
}

.orchard-band > div {
  padding: 4px 18px;
  border-right: 1px solid #405249;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.orchard-band > div:last-child {
  border: 0;
}

.orchard-band span {
  font-size: 10px;
  color: #a5b5ac;
}

.orchard-band strong {
  font-size: 17px;
}

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
  padding: 18px;
  min-height: 112px;
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
}

.stat-card .trend {
  font-size: 11px;
  color: var(--green);
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(300px, 0.8fr);
  gap: 18px;
}

.main-columns {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.side-columns {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel {
  background: white;
  border: 1px solid var(--line);
  border-radius: 6px;
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
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-header h3 .el-icon {
  color: var(--green);
}

.panel-body {
  padding: 18px;
}

.muted {
  color: var(--muted);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  border-radius: 4px;
  background: #edf4ef;
  color: var(--green);
  font-size: 11px;
  font-weight: 700;
}

.detail-list {
  margin: 0;
  padding: 4px 18px;
}

.detail-list div {
  display: grid;
  grid-template-columns: 120px 1fr;
  padding: 14px 0;
  border-bottom: 1px solid var(--line);
}

.detail-list div:last-child {
  border-bottom: 0;
}

dt {
  color: var(--muted);
  font-size: 12px;
}

dd {
  margin: 0;
  font-size: 13px;
}

.timeline {
  padding: 22px 24px;
}

.timeline p {
  color: var(--muted);
  font-size: 11px;
  margin: 5px 0;
}

.edit-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 14px;
}

/* 天气网格 */
.weather-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 18px;
}

.weather-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: var(--green-light);
  border-radius: 6px;
}

.weather-date {
  font-size: 12px;
  color: var(--muted);
  font-weight: 600;
}

.weather-icon {
  font-size: 28px;
  line-height: 1;
}

.weather-desc {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
}

.weather-temp {
  font-size: 11px;
  color: var(--muted);
}

/* 任务列表 */
.task-list {
  padding: 12px;
}

.task-item {
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 6px;
  margin-bottom: 10px;
}

.task-item:last-child {
  margin-bottom: 0;
}

.task-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.priority {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 3px;
  font-weight: 600;
}

.priority-high {
  background: #fee;
  color: var(--red);
}

.priority-medium {
  background: #fff5e6;
  color: var(--amber);
}

.priority-low {
  background: #edf4ef;
  color: var(--green);
}

.task-status {
  font-size: 11px;
  color: var(--muted);
}

.task-item h4 {
  margin: 0 0 6px;
  font-size: 14px;
  color: var(--ink);
}

.task-item p {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--muted);
  line-height: 1.5;
}

.task-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.task-time {
  font-size: 11px;
  color: var(--muted);
}

/* 风险提示 */
.risk-panel {
  border-left: 3px solid var(--amber);
}

.risk-content {
  padding: 16px 18px;
}

.risk-content p {
  margin: 0 0 12px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--ink);
}

.risk-content p:last-child {
  margin-bottom: 0;
}

.risk-content p strong {
  color: var(--green);
}

.loading-placeholder {
  padding: 32px;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1000px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .weather-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 760px) {
  .orchard-band {
    grid-template-columns: 1fr 1fr;
  }

  .orchard-band > div {
    border-bottom: 1px solid #405249;
  }

  .stat-grid {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  .edit-grid {
    grid-template-columns: 1fr;
  }

  .weather-grid {
    grid-template-columns: 1fr;
  }
}
</style>