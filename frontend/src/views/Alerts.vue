<template>
  <div class="alerts-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>告警记录</span>
          <div class="header-actions">
            <el-button @click="handleMarkAllRead" :disabled="!unreadCount">
              <el-icon><Check /></el-icon>
              全部已读
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选 -->
      <el-form inline class="search-form">
        <el-form-item label="状态">
          <el-select v-model="query.isRead" placeholder="请选择" clearable>
            <el-option label="未读" :value="0" />
            <el-option label="已读" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchAlerts">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="query.isRead = null; fetchAlerts()">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 告警列表 -->
      <div class="alert-list" v-loading="loading">
        <div
          v-for="alert in alerts"
          :key="alert.id"
          class="alert-item"
          :class="{ unread: !alert.isRead, [`risk-${alert.riskLevel}`]: true }"
          @click="handleRead(alert)"
        >
          <div class="alert-icon">
            <el-icon v-if="alert.alertType === 'cert'" color="#f56c6c"><Warning /></el-icon>
            <el-icon v-else color="#e6a23c"><Bell /></el-icon>
          </div>
          <div class="alert-content">
            <div class="alert-header">
              <span class="alert-title">{{ alert.title }}</span>
              <el-tag size="small" :type="getRiskTagType(alert.riskLevel)">
                {{ getRiskLevelText(alert.riskLevel) }}
              </el-tag>
              <span v-if="!alert.isRead" class="unread-dot"></span>
            </div>
            <div class="alert-message">{{ alert.message }}</div>
            <div class="alert-time">{{ formatTime(alert.sendTime) }}</div>
          </div>
        </div>

        <el-empty v-if="!loading && alerts.length === 0" description="暂无告警记录" />
      </div>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchAlerts"
        @current-change="fetchAlerts"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const alerts = ref([])
const loading = ref(false)
const total = ref(0)
const unreadCount = ref(0)

const query = reactive({
  page: 1,
  size: 20,
  isRead: null
})

onMounted(() => {
  fetchAlerts()
  fetchUnreadCount()
})

async function fetchAlerts() {
  loading.value = true
  try {
    const params = new URLSearchParams()
    params.append('page', query.page)
    params.append('size', query.size)
    if (query.isRead !== null) params.append('isRead', query.isRead)
    
    const res = await fetch(`/api/alerts?${params}`)
    const data = await res.json()
    alerts.value = data.list || []
    total.value = data.total || 0
    unreadCount.value = data.unread || 0
  } catch (e) {
    ElMessage.error('获取告警记录失败')
  } finally {
    loading.value = false
  }
}

async function fetchUnreadCount() {
  try {
    const res = await fetch('/api/alerts/unread-count')
    const data = await res.json()
    unreadCount.value = data.count || 0
  } catch (e) {
    console.error('获取未读数失败', e)
  }
}

async function handleRead(alert) {
  if (alert.isRead) return
  
  try {
    await fetch(`/api/alerts/${alert.id}/read`, { method: 'PUT' })
    alert.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (e) {
    console.error('标记已读失败', e)
  }
}

async function handleMarkAllRead() {
  try {
    await fetch('/api/alerts/read-all', { method: 'PUT' })
    ElMessage.success('已全部标记为已读')
    alerts.value.forEach(a => a.isRead = 1)
    unreadCount.value = 0
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

function getRiskTagType(level) {
  const types = { 0: 'success', 1: 'warning', 2: 'danger', 3: 'danger' }
  return types[level] || 'info'
}

function getRiskLevelText(level) {
  const texts = { 0: '正常', 1: '预警', 2: '高危', 3: '已过期' }
  return texts[level] || '未知'
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  return date.toLocaleString('zh-CN')
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.alert-list {
  min-height: 200px;
}

.alert-item {
  display: flex;
  gap: 15px;
  padding: 15px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  transition: background 0.2s;
}

.alert-item:hover {
  background: #f5f7fa;
}

.alert-item.unread {
  background: #ecf5ff;
}

.alert-item.unread:hover {
  background: #d9ecff;
}

.alert-icon {
  font-size: 24px;
  padding-top: 5px;
}

.alert-content {
  flex: 1;
}

.alert-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.alert-title {
  font-weight: bold;
  color: #333;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background: #409eff;
  border-radius: 50%;
}

.alert-message {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  margin-bottom: 8px;
}

.alert-time {
  color: #999;
  font-size: 12px;
}
</style>
