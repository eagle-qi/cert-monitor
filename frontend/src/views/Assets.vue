<template>
  <div class="assets-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>资产列表</span>
          <div class="header-actions">
            <el-button type="success" @click="handleBatchImport">
              <el-icon><Upload /></el-icon>
              批量导入
            </el-button>
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              添加资产
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <el-form inline class="search-form">
        <el-form-item label="业务分组">
          <el-select v-model="query.businessGroup" placeholder="请选择" clearable>
            <el-option v-for="g in groups" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="请选择" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchAssets">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 资产表格 -->
      <el-table :data="assets" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="url" label="URL" min-width="200" />
        <el-table-column prop="domain" label="域名" width="150" />
        <el-table-column prop="businessGroup" label="业务分组" width="120" />
        <el-table-column prop="owner" label="负责人" width="100" />
        <el-table-column prop="tags" label="标签" width="150">
          <template #default="{ row }">
            <el-tag v-for="tag in (row.tags || '').split(',')" :key="tag" size="small" style="margin-right: 5px">
              {{ tag }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleScan(row)">扫描</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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
        @size-change="fetchAssets"
        @current-change="fetchAssets"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="URL地址" required>
          <el-input v-model="form.url" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="协议类型">
          <el-select v-model="form.protocol" style="width: 100%">
            <el-option label="HTTPS" value="https" />
            <el-option label="HTTP" value="http" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务分组">
          <el-input v-model="form.businessGroup" placeholder="请输入业务分组" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.owner" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="多个标签用逗号分隔" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="批量导入" width="600px">
      <el-alert type="info" :closable="false" style="margin-bottom: 20px">
        每行一个URL，支持 http:// 和 https:// 开头
      </el-alert>
      <el-input
        v-model="importText"
        type="textarea"
        :rows="10"
        placeholder="https://example.com&#10;https://api.example.com&#10;https://www.example.org"
      />
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImportConfirm">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const assets = ref([])
const loading = ref(false)
const total = ref(0)
const groups = ref([])
const dialogVisible = ref(false)
const importDialogVisible = ref(false)
const dialogTitle = ref('添加资产')
const importText = ref('')

const query = reactive({
  page: 1,
  size: 10,
  businessGroup: null,
  status: null
})

const form = reactive({
  id: null,
  url: '',
  protocol: 'https',
  businessGroup: '',
  owner: '',
  tags: '',
  status: 1,
  isWhitelist: 1
})

onMounted(() => {
  fetchAssets()
  fetchGroups()
})

async function fetchAssets() {
  loading.value = true
  try {
    const params = new URLSearchParams()
    params.append('page', query.page)
    params.append('size', query.size)
    if (query.businessGroup) params.append('businessGroup', query.businessGroup)
    if (query.status) params.append('status', query.status)
    
    const res = await fetch(`/api/assets?${params}`)
    const data = await res.json()
    assets.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error('获取资产列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchGroups() {
  try {
    const res = await fetch('/api/assets/groups')
    groups.value = await res.json()
  } catch (e) {
    console.error('获取分组失败', e)
  }
}

function handleReset() {
  query.businessGroup = null
  query.status = null
  query.page = 1
  fetchAssets()
}

function handleAdd() {
  dialogTitle.value = '添加资产'
  Object.assign(form, { id: null, url: '', protocol: 'https', businessGroup: '', owner: '', tags: '', status: 1 })
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑资产'
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.url) {
    ElMessage.warning('请输入URL地址')
    return
  }
  
  try {
    const method = form.id ? 'PUT' : 'POST'
    const url = form.id ? `/api/assets/${form.id}` : '/api/assets'
    await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form)
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchAssets()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除资产 "${row.url}" 吗？`, '提示', {
      type: 'warning'
    })
    await fetch(`/api/assets/${row.id}`, { method: 'DELETE' })
    ElMessage.success('删除成功')
    fetchAssets()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

async function handleScan(row) {
  try {
    await fetch(`/api/scan/start/${row.id}`, { method: 'POST' })
    ElMessage.success('扫描已启动')
  } catch (e) {
    ElMessage.error('扫描启动失败')
  }
}

function handleBatchImport() {
  importText.value = ''
  importDialogVisible.value = true
}

async function handleImportConfirm() {
  if (!importText.value.trim()) {
    ElMessage.warning('请输入URL列表')
    return
  }
  
  const urls = importText.value.split('\n').filter(u => u.trim())
  if (urls.length === 0) {
    ElMessage.warning('请输入有效的URL')
    return
  }
  
  const assets = urls.map(url => ({
    url: url.trim(),
    protocol: url.trim().startsWith('https') ? 'https' : 'http',
    status: 1,
    isWhitelist: 1
  }))
  
  try {
    await fetch('/api/assets/batch', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(assets)
    })
    ElMessage.success(`成功导入 ${assets.length} 条资产`)
    importDialogVisible.value = false
    fetchAssets()
  } catch (e) {
    ElMessage.error('导入失败')
  }
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
</style>
