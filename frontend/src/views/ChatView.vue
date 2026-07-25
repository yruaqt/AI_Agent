<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import api, { unwrap } from '@/api'
import type { Orchard, PageData } from '@/types'
import {
  Plus, Delete, Promotion, VideoPause, Document,
  Loading, Check, Close, Expand, CopyDocument, RefreshRight, CaretBottom
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

// ── 类型定义 ──
interface Citation {
  documentId?: string
  documentName?: string
  chunkId?: string
  chunkNo?: number
  page?: number
  quote?: string
  content?: string
}

interface ToolCall {
  name: string
  status: string
  summary?: string | Record<string, any>
}

interface Message {
  id?: string
  _ts: number
  role: string
  content: string
  citations?: Citation[]
  tools?: ToolCall[]
  streaming?: boolean
  error?: boolean
  lastQuestion?: string
}

// ── 响应式状态 ──
const orchard = ref<Orchard | null>(null)
const sessions = ref<any[]>([])
const activeSession = ref<string>()
const messages = ref<Message[]>([])
const question = ref('')
const sending = ref(false)
const scrollRef = ref<HTMLElement>()
const controller = ref<AbortController>()
const sessionPaneVisible = ref(false)
const initLoading = ref(false)
const initError = ref<string | null>(null)
const sessionError = ref<string | null>(null)

// ── 滚动控制 ──
let isNearBottom = true
const SCROLL_THRESHOLD = 80
let scrollRafId: number | null = null

function onScroll() {
  if (!scrollRef.value) return
  const { scrollTop, scrollHeight, clientHeight } = scrollRef.value
  isNearBottom = scrollHeight - scrollTop - clientHeight < SCROLL_THRESHOLD
}

function scrollToBottom(force = false) {
  if (scrollRafId !== null) cancelAnimationFrame(scrollRafId)
  scrollRafId = requestAnimationFrame(() => {
    scrollRafId = null
    if (!scrollRef.value) return
    if (force || isNearBottom) {
      scrollRef.value.scrollTop = scrollRef.value.scrollHeight
    }
  })
}

// ── Markdown 渲染 ──
marked.setOptions({ breaks: true, gfm: true })

const markedRenderer = new marked.Renderer()
markedRenderer.link = function({ href, text }) {
  return `<a href="${href}" target="_blank" rel="noopener">${text}</a>`
}
marked.use({ renderer: markedRenderer })

function renderMarkdown(content: string): string {
  if (!content) return ''
  let rawHtml = marked.parse(content, { async: false }) as string
  rawHtml = rawHtml.replace(
    /<pre><code([^>]*)>([\s\S]*?)<\/code><\/pre>/g,
    '<pre><code$1>$2</code><button class="copy-code-btn" onclick="navigator.clipboard.writeText(this.previousElementSibling?.textContent||\'\')">复制</button></pre>'
  )
  return DOMPurify.sanitize(rawHtml, {
    ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'code', 'pre', 'ul', 'ol', 'li',
      'blockquote', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'table', 'thead',
      'tbody', 'tr', 'th', 'td', 'a', 'span', 'del', 'hr', 'button'],
    ALLOWED_ATTR: ['href', 'target', 'rel', 'class', 'onclick']
  })
}

// ── 初始化 ──
async function init() {
  const savedId = localStorage.getItem('currentOrchardId')
  initLoading.value = true
  initError.value = null
  try {
    const data = unwrap<PageData<Orchard>>(
      await api.get('/orchards', { params: { pageSize: 50, status: 'ENABLED' } })
    )
    if (savedId) {
      orchard.value = data.items.find((o: Orchard) => o.id === savedId) || data.items[0] || null
    } else {
      orchard.value = data.items[0] || null
    }
    await loadSessions()
    if (sessions.value[0]) await select(sessions.value[0].sessionId)
  } catch (e: any) {
    initError.value = e?.message || '初始化失败，请稍后重试'
  } finally {
    initLoading.value = false
  }
}

async function loadSessions() {
  sessionError.value = null
  try {
    const params: any = {}
    if (orchard.value) params.orchardId = orchard.value.id
    sessions.value = unwrap<PageData<any>>(await api.get('/chat/sessions', { params })).items
  } catch (e: any) {
    sessionError.value = e?.message || '加载会话列表失败'
    sessions.value = []
  }
}

async function createSession() {
  if (!orchard.value) {
    ElMessage.warning('请先选择果园')
    return
  }
  const s = unwrap<any>(
    await api.post('/chat/sessions', { orchardId: orchard.value.id, title: '新对话' })
  )
  sessions.value.unshift(s)
  activeSession.value = s.sessionId
  messages.value = []
}

async function select(id: string) {
  // P0: 切换会话前终止旧 SSE 请求
  stop()
  activeSession.value = id
  sessionPaneVisible.value = false
  isNearBottom = true
  try {
    const d = unwrap<PageData<any>>(await api.get(`/chat/sessions/${id}/messages?pageSize=50`))
    messages.value = d.items.map((m: any, i: number) => ({
      ...m,
      _ts: Date.now() + i,
      role: normalizeRole(m.role, i, d.items.length),
      content: m.content || m.answer || '',
      citations: m.citationsJson
        ? safeParse(m.citationsJson, [])
        : m.citations || [],
      tools: m.toolsJson
        ? safeParse(m.toolsJson, [])
        : m.toolCalls || m.tools || []
    }))
  } catch {
    messages.value = []
  }
  scrollToBottom(true)
}

function safeParse(str: string, fallback: any) {
  try {
    return JSON.parse(str)
  } catch {
    return fallback
  }
}

/** 将后端 role 值统一为 'user' | 'assistant'，兼容大写、缺失等情况 */
function normalizeRole(role: string | undefined, index: number, total: number): 'user' | 'assistant' {
  if (role) {
    const r = role.toLowerCase()
    if (r === 'user' || r === 'human' || r === 'me') return 'user'
    if (r === 'assistant' || r === 'ai' || r === 'bot' || r === 'system') return 'assistant'
  }
  // role 缺失时按奇偶推断：偶数索引为 user，奇数为 assistant
  return index % 2 === 0 ? 'user' : 'assistant'
}

const toolLabelMap: Record<string, string> = {
  calculateFertilizer: '肥料用量计算',
  queryOrchardWeather: '果园天气查询',
  getOrchardContext: '果园信息查询',
  queryKnowledge: '知识库检索',
  queryPests: '病虫害查询',
  calculateYield: '产量估算'
}
function toolLabel(name: string): string {
  return toolLabelMap[name] || name
}

async function remove(id: string) {
  try {
    await ElMessageBox.confirm('确定删除该会话吗？删除后无法恢复。', '删除会话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.delete(`/chat/sessions/${id}`)
    sessions.value = sessions.value.filter((s) => s.sessionId !== id)
    if (activeSession.value === id) {
      stop()
      activeSession.value = undefined
      messages.value = []
    }
  } catch (e: any) {
    // 用户取消删除，忽略
  }
}

// ── 发送消息（SSE 流式） ──
let readerRef: ReadableStreamDefaultReader<Uint8Array> | null = null
async function send(regenerateText?: string) {
  const text = (regenerateText || question.value).trim()
  if (!text || sending.value) return
  sending.value = true
  readerRef = null
  if (!activeSession.value) await createSession()
  if (!activeSession.value) {
    sending.value = false
    return
  }

  if (!regenerateText) question.value = ''

  messages.value.push({ _ts: Date.now(), role: 'user', content: text })
  messages.value.push({
    _ts: Date.now() + 1,
    role: 'assistant',
    content: '',
    citations: [],
    tools: [],
    streaming: true,
    lastQuestion: text
  })
  const target = messages.value[messages.value.length - 1]
  controller.value = new AbortController()
  isNearBottom = true
  scrollToBottom(true)

  try {
    const token = localStorage.getItem('accessToken')
    const base = import.meta.env.VITE_API_BASE || '/api/v1'
    const response = await fetch(
      `${base}/chat/sessions/${activeSession.value}/messages/stream`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ message: text }),
        signal: controller.value.signal
      }
    )

    // P0: SSE 401 处理 —— 跳转登录
    if (response.status === 401) {
      localStorage.removeItem('accessToken')
      ElMessage.warning('登录已过期，请重新登录')
      setTimeout(() => { window.location.href = '/login' }, 800)
      throw new Error('登录已过期')
    }
    if (!response.ok) {
      const err = await response.json().catch(() => ({}))
      throw new Error(err.message || `请求失败 (${response.status})`)
    }

    const reader = response.body!.getReader()
    readerRef = reader
    const decoder = new TextDecoder()
    let buffer = ''
    let firstDelta = true
    let doneReceived = false

    while (!doneReceived) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const blocks = buffer.split('\n\n')
      buffer = blocks.pop() || ''

      for (const block of blocks) {
        let event = 'message', data = ''
        for (const line of block.split('\n')) {
          if (line.startsWith('event:')) event = line.slice(6).trim()
          if (line.startsWith('data:')) data += line.slice(5).trim()
        }
        if (!data) continue

        // P1: JSON.parse 异常保护
        let parsed: any
        try {
          parsed = JSON.parse(data)
        } catch {
          continue
        }

        if (event === 'delta') {
          if (firstDelta) {
            target.content = ''
            firstDelta = false
          }
          target.content += parsed.content || ''
        }
        if (event === 'citation') target.citations!.push(parsed)
        if (event === 'tool_call') {
          target.tools!.push({
            name: parsed.name,
            status: parsed.status || 'RUNNING',
            summary: ''
          })
        }
        if (event === 'tool_result') {
          const existing = target.tools!.find(
            (t) => t.name === parsed.name && t.status === 'RUNNING'
          )
          let summary: string | Record<string, any> = parsed.summary || ''
          if (typeof summary === 'string') {
            try {
              summary = JSON.parse(summary)
            } catch {
              // 保持字符串原样
            }
          }
          if (existing) {
            existing.status = parsed.status || 'SUCCESS'
            existing.summary = summary
          } else {
            target.tools!.push({
              name: parsed.name,
              status: parsed.status || 'SUCCESS',
              summary
            })
          }
        }
        if (event === 'error') throw new Error(parsed.message || '生成失败')
        if (event === 'done') {
          doneReceived = true
          break
        }
        scrollToBottom()
      }
    }
    await loadSessions()
  } catch (e: any) {
    if (e.name === 'AbortError') {
      // P3: 用户主动终止，标记内容不完整
      if (target.content) {
        target.content += '\n\n⚠️ [已停止生成]'
      }
    } else {
      // P3: 网络断连标记
      if (target.content) {
        target.content += '\n\n⚠️ [内容因网络中断可能不完整]\n\n'
      }
      target.content += `生成失败：${e.message}`
      target.error = true
      ElMessage.error(e.message)
    }
  } finally {
    // 释放 reader，防止连接泄漏导致后续请求超时
    try { readerRef?.cancel() } catch {}
    // P1: 空内容处理
    if (!target.content.trim()) {
      target.content = '（AI 未返回有效内容，请重试）'
      target.error = true
    }
    target.streaming = false
    sending.value = false
    controller.value = undefined
  }
}

function stop() {
  if (controller.value) {
    controller.value.abort()
    controller.value = undefined
  }
  try { readerRef?.cancel() } catch {}
  readerRef = null
  sending.value = false
}

// ── 操作按钮 ──
function copyMessage(m: Message) {
  navigator.clipboard.writeText(m.content).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

function regenerate(m: Message) {
  if (!m.lastQuestion) return
  // 删除这条失败的/旧的回答
  const idx = messages.value.findIndex(x => x._ts === m._ts)
  if (idx >= 0) messages.value.splice(idx, 1)
  send(m.lastQuestion)
}

async function clearConversation() {
  if (!messages.value.length) return
  try {
    await ElMessageBox.confirm('确定清空当前对话的所有消息吗？', '清空对话', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    messages.value = []
  } catch {
    // 用户取消
  }
}

// ── 输入框 ──
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.metaKey) {
    e.preventDefault()
    send()
  } else if (e.key === 'Enter' && e.ctrlKey) {
    e.preventDefault()
    send()
  }
}

// ── 快捷提问 ──
function quickAsk(q: string) {
  question.value = q
  send()
}

// ── 生命周期 ──
onMounted(() => init().catch(() => {}))

// P0: 组件卸载时终止 SSE 请求
onBeforeUnmount(() => {
  stop()
})
</script>

<template>
  <div class="chat-workspace">
    <!-- 移动端遮罩 -->
    <div
      v-if="sessionPaneVisible"
      class="mobile-overlay"
      @click="sessionPaneVisible = false"
    />

    <aside class="session-pane" :class="{ show: sessionPaneVisible }">
      <el-button type="primary" :icon="Plus" @click="createSession" :disabled="initLoading">新建会话</el-button>

      <template v-if="initLoading">
        <div class="session-skeleton">
          <div v-for="i in 5" :key="i" class="skeleton-item">
            <div class="skeleton-line w80"></div>
            <div class="skeleton-line w40"></div>
          </div>
        </div>
      </template>
      <template v-else-if="initError">
        <div class="session-error">
          <p>{{ initError }}</p>
          <el-button size="small" @click="init">重试</el-button>
        </div>
      </template>
      <template v-else-if="sessionError">
        <div class="session-error">
          <p>{{ sessionError }}</p>
          <el-button size="small" @click="loadSessions">重试</el-button>
        </div>
      </template>
      <template v-else>
        <div v-if="!sessions.length" class="session-empty">暂无会话</div>
        <div class="session-list">
          <button
            v-for="s in sessions"
            :key="s.sessionId"
            :class="{ active: activeSession === s.sessionId }"
            @click="select(s.sessionId)"
          >
            <span>{{ s.title || '新对话' }}</span>
            <small>{{ s.createdAt?.slice(5, 16).replace('T', ' ') }}</small>
            <el-icon title="删除会话" @click.stop="remove(s.sessionId)"><Delete /></el-icon>
          </button>
        </div>
      </template>
    </aside>

    <section class="chat-main">
      <header class="chat-header">
        <div class="chat-header-left">
          <button class="session-toggle mobile-only" @click="sessionPaneVisible = !sessionPaneVisible">
            <el-icon><Expand /></el-icon>
          </button>
          <div class="header-info">
            <strong>{{ initLoading ? '加载中...' : (orchard?.name || '未选择果园') }}</strong>
            <span v-if="!initLoading">{{ orchard?.currentPhenology }} · RAG 已连接</span>
            <span v-else class="muted">正在初始化...</span>
          </div>
        </div>
        <div class="chat-header-right">
          <button
            v-if="messages.length && !sending"
            class="header-action-btn"
            title="清空对话"
            @click="clearConversation"
          >
            <el-icon><Delete /></el-icon>
          </button>
          <span class="status-pill">Agent 在线</span>
        </div>
      </header>

      <div ref="scrollRef" class="messages" @scroll="onScroll">
        <template v-if="initLoading">
          <div class="skeleton-messages">
            <div class="skeleton-msg assistant">
              <div class="sk-avatar"></div>
              <div class="sk-body">
                <div class="sk-line w30"></div>
                <div class="sk-line w100"></div>
                <div class="sk-line w80"></div>
                <div class="sk-line w50"></div>
              </div>
            </div>
          </div>
        </template>
        <template v-else-if="initError">
          <div class="chat-error-state">
            <p class="error-title">初始化失败</p>
            <p class="error-desc">{{ initError }}</p>
            <el-button type="primary" @click="init">重新加载</el-button>
          </div>
        </template>
        <template v-else>
          <div v-if="!messages.length" class="chat-empty">
            <div class="olive-seal">榄</div>
            <h2>今天需要了解什么？</h2>
            <div class="suggestions">
              <button @click="quickAsk('未来两天有大雨，幼果期是否需要灌溉和施肥？')">雨前水肥安排</button>
              <button @click="quickAsk('300株橄榄树，每株施肥12千克，总量是多少？')">肥料总量计算</button>
              <button @click="quickAsk('近期幼果落果较多，应先检查什么？')">幼果落果排查</button>
            </div>
          </div>

          <article
            v-for="m in messages"
            :key="m._ts"
            :class="['message', m.role, { error: m.error }]"
          >
            <div class="avatar">{{ m.role === 'user' ? '我' : '榄' }}</div>
            <div class="message-body">
              <div class="message-label">
                {{ m.role === 'user' ? '我的问题' : '榄园知行 Agent' }}
              </div>

              <!-- P2: 思考中占位 -->
              <div v-if="m.streaming && !m.content" class="thinking">
                <span class="think-dot"></span>
                <span class="think-dot"></span>
                <span class="think-dot"></span>
                正在思考...
              </div>

              <!-- P0: Markdown 渲染 -->
              <div
                v-else
                class="message-content markdown-body"
                v-html="renderMarkdown(m.content)"
              ></div>

              <div v-if="m.tools?.length" class="tool-list">
                <div
                  v-for="(t, ti) in m.tools"
                  :key="t.name + '-' + ti"
                  :class="['tool-chip', (t.status || '').toLowerCase()]"
                >
                  <div class="tool-header" @click="t._expanded = !t._expanded">
                    <el-icon v-if="t.status === 'RUNNING'" class="is-loading"><Loading /></el-icon>
                    <el-icon v-else-if="t.status === 'SUCCESS'"><Check /></el-icon>
                    <el-icon v-else><Close /></el-icon>
                    <span class="tool-name">{{ toolLabel(t.name) }}</span>
                    <span class="tool-status-text">
                      {{ t.status === 'RUNNING' ? '调用中' : t.status === 'SUCCESS' ? '已完成' : '失败' }}
                    </span>
                    <el-icon class="tool-expand-icon" :class="{ expanded: t._expanded }">
                      <CaretBottom />
                    </el-icon>
                  </div>
                  <div v-if="t._expanded && t.summary" class="tool-summary">
                    <div v-if="typeof t.summary === 'object'" class="summary-json">
                      <pre>{{ JSON.stringify(t.summary, null, 2) }}</pre>
                    </div>
                    <span v-else>{{ t.summary }}</span>
                  </div>
                </div>
              </div>

              <details v-if="m.citations?.length" class="citations">
                <summary>
                  <el-icon><Document /></el-icon>
                  {{ m.citations.length }} 条知识来源
                </summary>
                <div
                  v-for="(c, ci) in m.citations"
                  :key="ci"
                >
                  <strong>
                    {{ c.documentName || '未知来源' }} ·
                    {{ c.page ? '第 ' + c.page + ' 页' : '片段 ' + (c.chunkId || c.chunkNo || '—') }}
                  </strong>
                  <p>{{ c.quote || c.content }}</p>
                </div>
              </details>

              <!-- P2: AI 回复操作按钮 -->
              <div
                v-if="m.role === 'assistant' && !m.streaming && m.content"
                class="message-actions"
              >
                <button class="action-btn" title="复制" @click="copyMessage(m)">
                  <el-icon><CopyDocument /></el-icon>
                </button>
                <button
                  v-if="m.error || m.lastQuestion"
                  class="action-btn"
                  title="重新生成"
                  @click="regenerate(m)"
                >
                  <el-icon><RefreshRight /></el-icon>
                </button>
              </div>

              <!-- 用户消息复制按钮 -->
              <div
                v-if="m.role === 'user' && !m.streaming"
                class="message-actions"
              >
                <button class="action-btn" title="复制" @click="copyMessage(m)">
                  <el-icon><CopyDocument /></el-icon>
                </button>
              </div>
            </div>
          </article>
        </template>
      </div>

      <footer class="composer">
        <el-input
          v-model="question"
          type="textarea"
          :autosize="{ minRows: 2, maxRows: 5 }"
          placeholder="输入果园管理问题…（Enter 发送，Shift+Enter 换行）"
          resize="none"
          @keydown="onKeydown"
        />
        <button v-if="sending" class="send-button stop" title="停止生成" @click="stop">
          <el-icon><VideoPause /></el-icon>
        </button>
        <button v-else class="send-button" title="发送" :disabled="!question.trim()" @click="send()">
          <el-icon><Promotion /></el-icon>
        </button>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.chat-workspace {
  height: calc(100vh - 140px);
  min-height: 600px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 6px;
  display: grid;
  grid-template-columns: 230px 1fr;
  overflow: hidden;
  position: relative;
}

/* ── 侧栏 ── */
.session-pane {
  background: #f7f9f7;
  border-right: 1px solid var(--line);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.session-empty {
  color: var(--muted);
  font-size: 12px;
  text-align: center;
  padding: 20px 0;
}
.session-error {
  color: var(--red);
  font-size: 12px;
  text-align: center;
  padding: 16px 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
}
.session-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: auto;
}
.session-list button {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 3px 6px;
  border: 0;
  background: transparent;
  border-radius: 5px;
  padding: 11px;
  text-align: left;
  color: var(--ink);
  position: relative;
}
.session-list button.active,
.session-list button:hover {
  background: #e7eee9;
}
.session-list span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 12px;
}
.session-list small {
  color: var(--muted);
  font-size: 10px;
}
.session-list .el-icon {
  grid-column: 2;
  grid-row: 1 / 3;
  color: #88948d;
  align-self: center;
  padding: 4px;
  border-radius: 4px;
}
.session-list .el-icon:hover {
  background: #dce5df;
  color: var(--red);
}

/* ── 骨架屏 ── */
.session-skeleton {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.skeleton-item {
  padding: 11px;
  border-radius: 5px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.skeleton-line {
  height: 10px;
  border-radius: 4px;
  background: linear-gradient(90deg, #e8ecea 25%, #f0f3f1 50%, #e8ecea 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
.w30 { width: 30%; }
.w40 { width: 40%; }
.w50 { width: 50%; }
.w80 { width: 80%; }
.w100 { width: 100%; }
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ── 聊天主区域 ── */
.chat-main {
  min-width: 0;
  min-height: 0;
  display: grid;
  grid-template-rows: 64px 1fr auto;
  overflow: hidden;
}
.chat-header {
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}
.chat-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.chat-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.header-action-btn {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 5px;
  color: var(--muted);
  transition: all 0.2s;
}
.header-action-btn:hover {
  border-color: var(--red);
  color: var(--red);
}
.session-toggle {
  display: none;
  width: 32px;
  height: 32px;
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 5px;
  align-items: center;
  justify-content: center;
  color: var(--muted);
}
.session-toggle:hover {
  border-color: var(--green);
  color: var(--green);
}
.header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.chat-header strong { font-size: 13px; }
.chat-header span:not(.status-pill) { font-size: 10px; color: var(--muted); }

/* ── 消息列表 ── */
.messages {
  overflow-y: auto;
  min-height: 0;
  padding: 20px max(20px, 8%);
  scroll-behavior: smooth;
}
.chat-empty {
  display: grid;
  place-items: center;
  padding-top: 10vh;
  text-align: center;
}
.olive-seal {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  background: #e7efe9;
  color: var(--green);
  font: 700 25px serif;
  border-radius: 6px;
}
.chat-empty h2 { font-size: 20px; margin: 16px; }
.suggestions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}
.suggestions button {
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 5px;
  padding: 9px 12px;
  color: var(--muted);
  font-size: 11px;
}
.suggestions button:hover {
  border-color: var(--green);
  color: var(--green);
}

/* ── 消息项 ── */
.message {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}
.message.user {
  flex-direction: row-reverse;
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e8ede9;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}
.assistant .avatar {
  background: var(--green);
  color: white;
  font-family: serif;
}
.user .avatar {
  background: #4a8eff;
  color: white;
}
.message-body {
  max-width: 75%;
}
.message-label {
  font-size: 11px;
  color: var(--muted);
  margin: 0 0 7px;
}
.message-content {
  line-height: 1.8;
  font-size: 14px;
  word-break: break-word;
}
.assistant .message-content {
  background: #f7f9f7;
  border-radius: 12px 12px 12px 0;
  padding: 10px 14px;
}
.user .message-content {
  background: #4a8eff;
  color: white;
  border-radius: 12px 12px 0 12px;
  padding: 10px 14px;
}
.message.error .message-content {
  color: var(--red);
}
.user .message.error .message-content {
  color: #ffcad4;
}

/* ── 思考中动画 ── */
.thinking {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--muted);
  font-size: 13px;
  padding: 4px 0;
}
.think-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--green);
  animation: think-bounce 1.4s infinite ease-in-out;
}
.think-dot:nth-child(2) { animation-delay: 0.2s; }
.think-dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes think-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* ── 工具调用 ── */
.tool-list {
  display: flex;
  gap: 7px;
  flex-wrap: wrap;
  margin-top: 12px;
}
.tool-chip {
  display: flex;
  flex-direction: column;
  font-size: 12px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: #fff;
  overflow: hidden;
  min-width: 160px;
}
.tool-chip.running { border-color: #cfe8d5; color: var(--green); }
.tool-chip.success { border-color: #cfe8d5; color: var(--green); }
.tool-chip.failed, .tool-chip.error { border-color: #f3c9c6; color: var(--red); }
.tool-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: #f7f9f7;
  font-size: 12px;
  cursor: pointer;
  user-select: none;
}
.tool-chip.running .tool-header,
.tool-chip.success .tool-header { background: #eef4ef; }
.tool-chip.failed .tool-header,
.tool-chip.error .tool-header { background: #f9eceb; }
.tool-name {
  font-weight: 600;
  flex: 1;
}
.tool-status-text {
  font-size: 11px;
  opacity: 0.8;
}
.tool-expand-icon {
  font-size: 11px;
  transition: transform 0.2s;
  opacity: 0.6;
}
.tool-expand-icon.expanded {
  transform: rotate(180deg);
}
.tool-summary {
  padding: 8px 10px;
  font-size: 11px;
  color: var(--muted);
  border-top: 1px solid var(--line);
}
.summary-json pre {
  margin: 0;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 11px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--ink);
  background: #f7f9f7;
  padding: 8px;
  border-radius: 4px;
  max-height: 200px;
  overflow-y: auto;
}

/* ── 引用来源 ── */
.citations {
  margin-top: 12px;
  border-top: 1px solid var(--line);
  padding-top: 10px;
}
.citations summary {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--green);
  font-size: 11px;
  cursor: pointer;
}
.citations div {
  background: #f7f9f7;
  border-left: 3px solid #a9c56d;
  margin-top: 8px;
  padding: 10px;
}
.citations strong { font-size: 11px; }
.citations p { font-size: 11px; color: var(--muted); line-height: 1.5; margin: 5px 0 0; }

/* ── 操作按钮 ── */
.message-actions {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}
.message:hover .message-actions {
  opacity: 1;
}
.action-btn {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 4px;
  color: var(--muted);
  font-size: 14px;
  transition: all 0.2s;
}
.action-btn:hover {
  border-color: var(--green);
  color: var(--green);
  background: var(--green-light);
}

/* ── 输入框 ── */
.composer {
  border-top: 1px solid var(--line);
  padding: 14px max(20px, 8%);
  display: grid;
  grid-template-columns: 1fr 42px;
  gap: 10px;
  align-items: end;
}
.send-button {
  height: 42px;
  width: 42px;
  border: 0;
  border-radius: 5px;
  background: var(--green);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 18px;
}
.send-button.stop { background: var(--red); }
.send-button:disabled { opacity: 0.45; }

/* ── 错误状态 ── */
.chat-error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  text-align: center;
  gap: 12px;
}
.chat-error-state .error-title { font-size: 15px; font-weight: 600; margin: 0; }
.chat-error-state .error-desc { font-size: 13px; color: var(--muted); margin: 0; }

/* ── 骨架屏消息 ── */
.skeleton-messages { padding: 20px 8%; }
.skeleton-msg {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}
.sk-avatar {
  width: 32px;
  height: 32px;
  border-radius: 5px;
  background: linear-gradient(90deg, #e8ecea 25%, #f0f3f1 50%, #e8ecea 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
.sk-body { display: flex; flex-direction: column; gap: 8px; }
.sk-line {
  height: 12px;
  border-radius: 4px;
  background: linear-gradient(90deg, #e8ecea 25%, #f0f3f1 50%, #e8ecea 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

/* ── 移动端遮罩 ── */
.mobile-overlay {
  display: none;
}

/* ── Markdown 渲染样式 ── */
.markdown-body :deep(p) { margin: 0 0 8px; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(strong) { font-weight: 700; }
.markdown-body :deep(em) { font-style: italic; }
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 16px 0 8px;
  font-weight: 600;
  line-height: 1.4;
}
.markdown-body :deep(h1) { font-size: 18px; }
.markdown-body :deep(h2) { font-size: 16px; }
.markdown-body :deep(h3) { font-size: 15px; }
.markdown-body :deep(h4) { font-size: 14px; }
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}
.markdown-body :deep(li) { margin: 4px 0; line-height: 1.7; }
.markdown-body :deep(blockquote) {
  margin: 8px 0;
  padding: 8px 14px;
  border-left: 3px solid var(--green);
  background: var(--green-light);
  color: var(--ink);
}
.markdown-body :deep(blockquote p) { margin: 0; }
.markdown-body :deep(code) {
  background: #f0f2f0;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 13px;
  font-family: 'Consolas', 'Monaco', monospace;
}
.markdown-body :deep(pre) {
  background: #1e2b25;
  color: #dce6df;
  padding: 14px 16px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 10px 0;
  font-size: 13px;
  line-height: 1.6;
  position: relative;
}
.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
  font-size: inherit;
}
.markdown-body :deep(.copy-code-btn) {
  position: absolute;
  top: 6px;
  right: 6px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #dce6df;
  font-size: 10px;
  padding: 3px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}
.markdown-body :deep(.copy-code-btn:hover) {
  background: rgba(255, 255, 255, 0.2);
}
.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 10px 0;
  font-size: 13px;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid var(--line);
  padding: 8px 12px;
  text-align: left;
}
.markdown-body :deep(th) {
  background: #f7f9f7;
  font-weight: 600;
}
.markdown-body :deep(a) {
  color: var(--green);
  text-decoration: underline;
}
.markdown-body :deep(hr) {
  border: 0;
  border-top: 1px solid var(--line);
  margin: 14px 0;
}
.markdown-body :deep(del) {
  color: var(--muted);
}

/* ── 移动端 ── */
@media (max-width: 800px) {
  .chat-workspace {
    grid-template-columns: 1fr;
    height: calc(100dvh - 116px);
  }
  .session-pane {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 230px;
    z-index: 20;
    transform: translateX(-100%);
    transition: transform 0.2s ease;
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.08);
  }
  .session-pane.show {
    transform: translateX(0);
  }
  .mobile-overlay {
    display: block;
    position: absolute;
    inset: 0;
    background: rgba(0, 0, 0, 0.3);
    z-index: 15;
  }
  .session-toggle {
    display: grid;
  }
  .messages {
    padding: 16px;
  }
  .composer {
    padding: 12px;
  }
  .chat-header {
    padding: 0 14px;
  }
  .message-actions {
    opacity: 1;
  }
}
</style>
