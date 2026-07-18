<script setup lang="ts">
import { DataLine, Document, Link, Setting } from '@element-plus/icons-vue'
</script>
<template><div class="shell"><aside><div class="brand"><span>榄</span><div><strong>榄园知行</strong><small>STARTER</small></div></div><nav><router-link to="/"><el-icon><DataLine/></el-icon>工程状态</router-link><a href="http://localhost:8080/swagger-ui/index.html" target="_blank"><el-icon><Link/></el-icon>Swagger</a><a href="http://localhost:8080/h2-console" target="_blank"><el-icon><Setting/></el-icon>H2 Console</a><a href="https://github.com" target="_blank"><el-icon><Document/></el-icon>团队仓库</a></nav><div class="aside-note">业务模块等待团队实现</div></aside><main><header><div><p>OLIVE AGENT ENGINEERING</p><h1>团队开发基线</h1></div><span class="version">V0.1.0</span></header><section><slot/></section></main></div></template>

