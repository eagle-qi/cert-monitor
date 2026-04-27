import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    ElMessage.error(error.message || '请求失败')
    return Promise.reject(error)
  }
)

export default {
  // 资产接口
  getAssets: (params) => api.get('/assets', { params }),
  getAsset: (id) => api.get(`/assets/${id}`),
  createAsset: (data) => api.post('/assets', data),
  updateAsset: (id, data) => api.put(`/assets/${id}`, data),
  deleteAsset: (id) => api.delete(`/assets/${id}`),
  batchImportAssets: (data) => api.post('/assets/batch', data),
  getAssetGroups: () => api.get('/assets/groups'),
  
  // 证书接口
  getCerts: (params) => api.get('/certs', { params }),
  getCert: (id) => api.get(`/certs/${id}`),
  getCertByAsset: (assetId) => api.get(`/certs/asset/${assetId}`),
  scanCert: (assetId) => api.post(`/certs/scan/${assetId}`),
  scanAllCerts: () => api.post('/certs/scan-all'),
  getExpiringCerts: (days) => api.get('/certs/expiring', { params: { days } }),
  getCertStats: () => api.get('/certs/stats'),
  
  // 扫描接口
  getScanResults: (params) => api.get('/scan/results', { params }),
  getLatestResult: (assetId) => api.get(`/scan/results/${assetId}/latest`),
  startScan: (assetId) => api.post(`/scan/start/${assetId}`),
  startAllScan: () => api.post('/scan/start-all'),
  getScanStats: () => api.get('/scan/stats'),
  
  // 告警接口
  getAlerts: (params) => api.get('/alerts', { params }),
  getUnreadCount: () => api.get('/alerts/unread-count'),
  markAsRead: (id) => api.put(`/alerts/${id}/read`),
  markAllAsRead: () => api.put('/alerts/read-all'),
  getAlertConfigs: () => api.get('/alerts/config'),
  getAlertConfig: (type) => api.get(`/alerts/config/${type}`),
  saveAlertConfig: (data) => api.post('/alerts/config', data),
  deleteAlertConfig: (id) => api.delete(`/alerts/config/${id}`),
  
  // 大盘接口
  getDashboardStats: () => api.get('/dashboard/stats'),
  getCertRiskDistribution: () => api.get('/dashboard/cert-risk-distribution'),
  getCertTrend: () => api.get('/dashboard/cert-trend'),
  getExpiringCerts: (params) => api.get('/dashboard/expiring-certs', { params }),
  getBusinessGroups: () => api.get('/dashboard/business-groups'),
  
  // 导出接口
  exportAssets: () => api.get('/export/assets', { responseType: 'blob' }),
  exportCerts: () => api.get('/export/certs', { responseType: 'blob' })
}
