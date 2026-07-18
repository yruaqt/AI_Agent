<script setup lang="ts">
import {computed,onMounted,ref} from 'vue'
import api,{unwrap} from '@/api'
import AppShell from '@/components/AppShell.vue'
import {Connection,RefreshRight,Warning} from '@element-plus/icons-vue'
interface DatabaseStatus{status:string;errorType?:string}
interface Status{application:string;version:string;database:DatabaseStatus;profiles:string[];serverTime:string}
interface ModuleItem{code:string;name:string;owner:string;status:string}
const status=ref<Status>(),modules=ref<ModuleItem[]>([]),loading=ref(false),error=ref('')
const connected=computed(()=>status.value?.database.status==='UP')
async function load(){loading.value=true;error.value='';try{const[s,m]=await Promise.all([api.get('/system/status'),api.get('/system/modules')]);status.value=unwrap<Status>(s);modules.value=unwrap<ModuleItem[]>(m)}catch(e){error.value='后端尚未启动，请进入 backend 目录运行 mvn spring-boot:run'}finally{loading.value=false}}
onMounted(load)
const detail:Record<string,string>={FRONTEND:'Vue 页面、路由、状态管理与 SSE 交互',AGENT_RAG:'LangChain4j、知识库、天气和任务生成',AUTH_USER:'JWT、角色权限、用户管理和操作日志',ORCHARD_BUSINESS:'果园、物候期、计算器和实训记录'}
</script>
<template><AppShell><div class="heading"><div><h2>工程连通性</h2><p>公共基础和数据库基线已就绪，业务代码由成员独立实现。</p></div><el-button :icon="RefreshRight" :loading="loading" @click="load">重新检测</el-button></div><el-alert v-if="error" :title="error" type="warning" :closable="false" show-icon/><div class="status-grid"><article><span>后端服务</span><strong :class="connected?'ok':'down'">{{status?'ONLINE':'OFFLINE'}}</strong><small>{{status?.application||'等待连接'}}</small></article><article><span>数据库</span><strong :class="connected?'ok':'down'">{{status?.database.status||'UNKNOWN'}}</strong><small>Flyway + JPA</small></article><article><span>运行配置</span><strong>{{status?.profiles.join(', ')||'--'}}</strong><small>H2 / PostgreSQL</small></article><article><span>业务模块</span><strong>{{modules.length||4}} 个</strong><small>均待团队实现</small></article></div><div class="section-title"><div><h2>开发分工</h2><p>每个模块具有唯一主要负责人。</p></div><span><el-icon><Connection/></el-icon> 基础接口已连接</span></div><div class="module-grid"><article v-for="(item,index) in modules" :key="item.code"><div class="module-index">0{{index+1}}</div><div class="module-content"><header><span>{{item.owner}}</span><b>{{item.status}}</b></header><h3>{{item.name}}</h3><p>{{detail[item.code]}}</p><div class="progress-line"><i></i></div></div></article></div><div class="rules"><el-icon><Warning/></el-icon><div><strong>开发边界</strong><p>Starter 不提供业务实现。新增接口必须使用统一响应、Flyway 迁移、Swagger 描述和自动化测试。</p></div></div></AppShell></template>
