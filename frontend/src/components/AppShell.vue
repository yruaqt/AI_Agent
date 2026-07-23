<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { DataAnalysis, ChatDotRound, Calendar, SetUp, Notebook, Collection, Files, User, Fold, Expand, SwitchButton } from '@element-plus/icons-vue'

const auth=useAuthStore(),route=useRoute(),router=useRouter(),collapsed=ref(false),mobileOpen=ref(false)
const menus=[
  {path:'/',label:'果园总览',icon:DataAnalysis},{path:'/chat',label:'智能问答',icon:ChatDotRound},
  {path:'/tasks',label:'今日农事',icon:Calendar},{path:'/calculators',label:'用量计算',icon:SetUp},
  {path:'/records',label:'实训记录',icon:Notebook},{path:'/orchards',label:'果园档案',icon:Collection},
  {path:'/knowledge',label:'知识库',icon:Files,admin:true},{path:'/users',label:'用户管理',icon:User,admin:true}
]
const visibleMenus=computed(()=>menus.filter(x=>!x.admin||auth.isAdmin)),title=computed(()=>menus.find(x=>x.path===route.path)?.label||'榄园知行')
onMounted(()=>auth.load().catch(()=>{}))
function go(path:string){router.push(path);mobileOpen.value=false}
function logout(){auth.logout();router.push('/login')}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" :class="{collapsed}">
      <button class="brand" title="返回果园总览" @click="go('/')"><span class="brand-mark">榄</span><span v-if="!collapsed" class="brand-copy"><strong>榄园知行</strong><small>OLIVE ORCHARD OS</small></span></button>
      <nav><button v-for="item in visibleMenus" :key="item.path" class="nav-item" :class="{active:route.path===item.path}" :title="item.label" @click="go(item.path)"><el-icon><component :is="item.icon"/></el-icon><span v-if="!collapsed">{{item.label}}</span></button></nav>
      <button class="collapse-button" :title="collapsed?'展开导航':'收起导航'" @click="collapsed=!collapsed"><el-icon><Expand v-if="collapsed"/><Fold v-else/></el-icon><span v-if="!collapsed">收起导航</span></button>
    </aside>
    <el-drawer v-model="mobileOpen" direction="ltr" size="260px" :with-header="false" class="mobile-drawer"><div class="mobile-menu"><div class="mobile-brand">榄园知行</div><button v-for="item in visibleMenus" :key="item.path" class="nav-item" :class="{active:route.path===item.path}" @click="go(item.path)"><el-icon><component :is="item.icon"/></el-icon><span>{{item.label}}</span></button></div></el-drawer>
    <main class="main-area">
      <header class="topbar"><button class="icon-button mobile-only" title="打开导航" @click="mobileOpen=true"><el-icon><Expand/></el-icon></button><div><p class="eyebrow">学校东区橄榄实训果园</p><h1>{{title}}</h1></div><div class="account"><span class="role-dot"></span><div><strong>{{auth.user?.displayName||'正在载入'}}</strong><small>{{auth.user?.role==='ADMIN'?'教师 / 管理员':'学生'}}</small></div><button class="icon-button" title="退出登录" @click="logout"><el-icon><SwitchButton/></el-icon></button></div></header>
      <section class="page-content"><router-view/></section>
    </main>
  </div>
</template>

