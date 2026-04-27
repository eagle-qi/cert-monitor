<template>
  <div class="certs-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>证书管理</span>
          <div class="header-actions">
            <el-button @click="handleExport">
              <el-icon><Download /></el-icon>
              导出报表
            </el-button>
            <el-button type="primary" @click="handleScanAll" :loading="scanning">
              <el-icon><Refresh /></el-icon>
              全量扫描
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选 -->
      <el-form inline class="search-form">
        <el-form-item label="风险等级">
          <el-select v-model="query.riskLevel" placeholder="请选择" clearable>
            <el-option label="正常" :value="0" />
            <el-option label="预警" :value="1" />
            <el-option label="高危" :value="2" />
            <el-option label="已过期" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchCerts">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="query.riskLevel = null; fetchCerts()">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :span="6">
          <div class="stat-item normal">
            <span class="stat-num">{{ stats.normal || 0 }}</span>
            <span class="stat-text">正常</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item warning">
            <span class="stat-num">{{ stats.warning || 0 }}</span>
            <span class="stat-text">预警</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item danger">
            <span class="stat-num">{{ stats.danger || 0 }}</span>
            <span class="stat-text">高危</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item expired">
            <span class="stat-num">{{ stats.expired || 0 }}</span>
            <span class="stat-text">已过期</span>
          </div>
        </el-col>
      </el-row>

      <!-- 证书表格 -->
      <el-table :data="certs" v-loading="loading" stripe>
        <el-table-column prop="assetId" label="资产ID" width="100" />
        <el-table-column prop="subject" label="证书主体" min-width="200" show-overflow-tooltip />
        <el-table-column prop="issuer" label="颁发机构" min-width="150" show-overflow-tooltip />
        <el-table-column prop="validStart" label="生效时间" width="160" />
        <el-table-column prop="validEnd" label="过期时间" width="160" />
        <el-table-column prop="remainDays" label="剩余天数" width="100">
          <template #default="{ row }">
            <span :style="{ color: getRemainDaysColor(row.remainDays) }">
              {{ row.remainDays }}天
            </span>
          </template>
        </el-table-column>
        <el-table-column label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getRiskTagType(row.riskLevel)">
              {{ getRiskLevelText(row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scanTime" label="扫描时间" width="160" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleRescan(row)">重新扫描</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchCerts"
        @current-change="fetchCerts"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const certs = ref([])
const stats = ref({})
const loading = ref(false)
const scanning = ref(false)
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  riskLevel: null
})

onMounted(() => {
  fetchCerts()
  fetchStats()
})

async function fetchCerts() {
  loading.value = true
  try {
    const params = new URLSearchParams()
    params.append('page', query.page)
    params.append('size', query.size)
    if (query.riskLevel !== null) params.append('riskLevel', query.riskLevel)
    
    const res = await fetch(`/api/certs?${params}`)
    const data = await res.json()
    certs.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error('获取证书列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  try {
    const res = await fetch('/api/certs/stats')
    stats.value = await res.json()
  } catch (e) {
    console.error('获取统计失败', e)
  }
}

async function handleScanAll() {
  scanning.value = true
  try {
    await fetch('/api/certs/scan-all', { method: 'POST' })
    ElMessage.success('全量扫描已启动')
    setTimeout(() => {
      fetchCerts()
      fetchStats()
    }, 3000)
  } catch (e) {
    ElMessage.error('扫描启动失败')
  } finally {
    scanning.value = false
  }
}

async function handleRescan(row) {
  try {
    await fetch(`/api/certs/scan/${row.assetId}`, { method: 'POST' })
    ElMessage.success('扫描已启动')
    setTimeout(fetchCerts, 2000)
  } catch (e) {
    ElMessage.error('扫描启动失败')
  }
}

async function handleExport() {
  try {
    const res = await fetch('/api/export/certs')
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `证书清单_${new Date().toISOString().split('T')[0]}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
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

function getRemainDaysColor(days) {
  if (days <= 0) return '#f56c6c'
  if (days <= 15) return '#f56c6c'
  if (days <= 30) return '#e6a23c'
  return '#67c23a'
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.search-form {
  margin-bottom: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-item {
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  color: #fff;
}

.stat-item.normal { background: #67c23a; }
.stat-item.warning { background: #e6a23c; }
.stat-item.danger { background: #f56c6c; }
.stat-item.expired { background: #909399; }

.stat-num {
  display: block;
  font-size: 28px;
  font-weight: bold;
}

.stat-text {
  display: block;
  font-size: 14px;
  margin-top: 5px;
}
</style>
