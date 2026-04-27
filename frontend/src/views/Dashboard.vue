<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #409eff">
            <el-icon><FolderOpened /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">资产总数</p>
            <p class="stat-value">{{ stats.totalAssets || 0 }}</p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #67c23a">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">可用率</p>
            <p class="stat-value">{{ (stats.accessibleRate || 0).toFixed(1) }}%</p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #e6a23c">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">预警证书</p>
            <p class="stat-value">{{ stats.warningCerts || 0 }}</p>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #f56c6c">
            <el-icon><Bell /></el-icon>
          </div>
          <div class="stat-info">
            <p class="stat-label">未读告警</p>
            <p class="stat-value">{{ stats.unreadAlerts || 0 }}</p>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 证书风险概览 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>证书风险分布</span>
            </div>
          </template>
          <div ref="riskChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>证书过期趋势</span>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 即将过期证书 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>即将过期证书</span>
          <el-button type="primary" link @click="$router.push('/certs')">查看更多</el-button>
        </div>
      </template>
      <el-table :data="expiringCerts" stripe>
        <el-table-column prop="domain" label="域名" width="200" />
        <el-table-column prop="subject" label="证书主体" width="200" />
        <el-table-column prop="issuer" label="颁发机构" width="200" />
        <el-table-column prop="validEnd" label="过期时间" width="160" />
        <el-table-column prop="remainDays" label="剩余天数" width="100">
          <template #default="{ row }">
            <el-tag :type="getRiskTagType(row.riskLevel)">
              {{ row.remainDays }}天
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getRiskTagType(row.riskLevel)">
              {{ getRiskLevelText(row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const stats = ref({})
const expiringCerts = ref([])
const riskChartRef = ref(null)
const trendChartRef = ref(null)
let riskChart = null
let trendChart = null

onMounted(async () => {
  await fetchStats()
  await fetchExpiringCerts()
  initCharts()
})

onUnmounted(() => {
  riskChart?.dispose()
  trendChart?.dispose()
})

async function fetchStats() {
  try {
    const res = await fetch('/api/dashboard/stats')
    stats.value = await res.json()
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
}

async function fetchExpiringCerts() {
  try {
    const res = await fetch('/api/dashboard/expiring-certs?days=30&page=1&size=5')
    const data = await res.json()
    expiringCerts.value = data.list || []
  } catch (e) {
    console.error('获取即将过期证书失败', e)
  }
}

function initCharts() {
  initRiskChart()
  initTrendChart()
}

async function initRiskChart() {
  if (!riskChartRef.value) return
  
  try {
    const res = await fetch('/api/dashboard/cert-risk-distribution')
    const data = await res.json()
    
    riskChart = echarts.init(riskChartRef.value)
    riskChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: '5%', left: 'center' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {c} ({d}%)' },
        data: data.data || []
      }]
    })
  } catch (e) {
    console.error('初始化风险图表失败', e)
  }
}

async function initTrendChart() {
  if (!trendChartRef.value) return
  
  try {
    const res = await fetch('/api/dashboard/cert-trend')
    const data = await res.json()
    
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['预警', '高危', '已过期'], bottom: '5%' },
      grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: data.data?.map(d => d.date) || [] },
      yAxis: { type: 'value' },
      series: [
        { name: '预警', type: 'line', data: data.data?.map(d => d.warning) || [], smooth: true, itemStyle: { color: '#e6a23c' } },
        { name: '高危', type: 'line', data: data.data?.map(d => d.danger) || [], smooth: true, itemStyle: { color: '#f56c6c' } },
        { name: '已过期', type: 'line', data: data.data?.map(d => d.expired) || [], smooth: true, itemStyle: { color: '#909399' } }
      ]
    })
  } catch (e) {
    console.error('初始化趋势图表失败', e)
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
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.stat-info {
  flex: 1;
}

.stat-label {
  margin: 0 0 5px;
  color: #999;
  font-size: 14px;
}

.stat-value {
  margin: 0;
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  height: 350px;
}

.chart-container {
  height: 280px;
}

.table-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
