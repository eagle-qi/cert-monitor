<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <el-icon><Lock /></el-icon>
        <span>CertMonitor</span>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>监控大盘</span>
        </el-menu-item>
        <el-menu-item index="/assets">
          <el-icon><FolderOpened /></el-icon>
          <span>资产管理</span>
        </el-menu-item>
        <el-menu-item index="/certs">
          <el-icon><Document /></el-icon>
          <span>证书管理</span>
        </el-menu-item>
        <el-menu-item index="/alerts">
          <el-icon><Bell /></el-icon>
          <span>告警记录</span>
          <el-badge :value="unreadCount" :hidden="!unreadCount" class="alert-badge" />
        </el-menu-item>
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <span>系统设置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-left">
          <h2>{{ currentTitle }}</h2>
        </div>
        <div class="header-right">
          <el-button type="primary" @click="handleScanAll" :loading="scanning">
            <el-icon><Refresh /></el-icon>
            立即扫描
          </el-button>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              管理员
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="settings">系统设置</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

const unreadCount = ref(0)
const scanning = ref(false)

const currentTitle = computed(() => {
  return route.meta.title || '监控大盘'
})

let pollTimer = null

onMounted(() => {
  fetchUnreadCount()
  // 每分钟轮询未读告警数
  pollTimer = setInterval(fetchUnreadCount, 60000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

async function fetchUnreadCount() {
  try {
    const res = await fetch('/api/alerts/unread-count')
    const data = await res.json()
    unreadCount.value = data.count || 0
  } catch (e) {
    // 忽略错误
  }
}

async function handleScanAll() {
  scanning.value = true
  try {
    await fetch('/api/scan/start-all', { method: 'POST' })
    ElMessage.success('全量扫描已启动')
    setTimeout(() => {
      window.location.reload()
    }, 2000)
  } catch (e) {
    ElMessage.error('扫描启动失败')
  } finally {
    scanning.value = false
  }
}

function handleCommand(command) {
  if (command === 'logout') {
    router.push('/login')
  } else if (command === 'settings') {
    router.push('/settings')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background: #1a1a2e;
  color: #fff;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
  border-bottom: 1px solid #2a2a4e;
}

.sidebar-menu {
  border-right: none;
  background: transparent;
}

.sidebar-menu .el-menu-item {
  color: #ccc;
}

.sidebar-menu .el-menu-item:hover,
.sidebar-menu .el-menu-item.is-active {
  background: #2a2a4e;
  color: #409eff;
}

.alert-badge {
  margin-left: auto;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.header-left h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  color: #666;
}

.main-content {
  background: #f5f7fa;
  padding: 20px;
}
</style>
