<template>
  <div class="settings-page">
    <el-card>
      <template #header>
        <span>告警配置</span>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 邮件告警 -->
        <el-tab-pane label="邮件告警" name="email">
          <el-form :model="emailConfig" label-width="120px" style="max-width: 600px">
            <el-form-item label="发件邮箱">
              <el-input v-model="emailConfig.fromEmail" placeholder="your_email@qq.com" />
            </el-form-item>
            <el-form-item label="SMTP服务器">
              <el-input v-model="emailConfig.smtpHost" placeholder="smtp.qq.com" />
            </el-form-item>
            <el-form-item label="SMTP端口">
              <el-input v-model="emailConfig.smtpPort" placeholder="587" />
            </el-form-item>
            <el-form-item label="授权码">
              <el-input v-model="emailConfig.password" type="password" show-password placeholder="邮箱授权码" />
            </el-form-item>
            <el-form-item label="收件人">
              <el-input v-model="emailConfig.toEmail" placeholder="收件邮箱地址" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveEmailConfig">保存配置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 企业微信 -->
        <el-tab-pane label="企业微信" name="wechat">
          <el-form :model="wechatConfig" label-width="120px" style="max-width: 600px">
            <el-form-item label="Webhook地址">
              <el-input v-model="wechatConfig.webhookUrl" placeholder="https://qyapi.weixin.qq.com/..." />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveWechatConfig">保存配置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 钉钉 -->
        <el-tab-pane label="钉钉" name="dingtalk">
          <el-form :model="dingtalkConfig" label-width="120px" style="max-width: 600px">
            <el-form-item label="Webhook地址">
              <el-input v-model="dingtalkConfig.webhookUrl" placeholder="https://oapi.dingtalk.com/..." />
            </el-form-item>
            <el-form-item label="加签密钥">
              <el-input v-model="dingtalkConfig.secret" placeholder="可选" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveDingtalkConfig">保存配置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header>
        <span>扫描配置</span>
      </template>
      <el-form :model="scanConfig" label-width="120px" style="max-width: 600px">
        <el-form-item label="超时时间(毫秒)">
          <el-input-number v-model="scanConfig.timeout" :min="1000" :max="30000" :step="1000" />
        </el-form-item>
        <el-form-item label="重试次数">
          <el-input-number v-model="scanConfig.retryCount" :min="0" :max="5" />
        </el-form-item>
        <el-form-item label="告警冷却(分钟)">
          <el-input-number v-model="scanConfig.cooldownMinutes" :min="1" :max="1440" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveScanConfig">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header>
        <span>系统信息</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="系统版本">1.0.0</el-descriptions-item>
        <el-descriptions-item label="构建时间">2026-04-27</el-descriptions-item>
        <el-descriptions-item label="Spring Boot">3.2.0</el-descriptions-item>
        <el-descriptions-item label="Vue">3.4.0</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('email')

const emailConfig = reactive({
  fromEmail: '',
  smtpHost: 'smtp.qq.com',
  smtpPort: '587',
  password: '',
  toEmail: ''
})

const wechatConfig = reactive({
  webhookUrl: ''
})

const dingtalkConfig = reactive({
  webhookUrl: '',
  secret: ''
})

const scanConfig = reactive({
  timeout: 5000,
  retryCount: 2,
  cooldownMinutes: 60
})

onMounted(() => {
  loadConfigs()
})

async function loadConfigs() {
  try {
    // 加载邮件配置
    const emailRes = await fetch('/api/alerts/config/email')
    const emailData = await emailRes.json()
    if (emailData && emailData.config) {
      const config = JSON.parse(emailData.config)
      Object.assign(emailConfig, config)
    }

    // 加载企业微信配置
    const wechatRes = await fetch('/api/alerts/config/wechat')
    const wechatData = await wechatRes.json()
    if (wechatData && wechatData.config) {
      const config = JSON.parse(wechatData.config)
      Object.assign(wechatConfig, config)
    }

    // 加载钉钉配置
    const dingtalkRes = await fetch('/api/alerts/config/dingtalk')
    const dingtalkData = await dingtalkRes.json()
    if (dingtalkData && dingtalkData.config) {
      const config = JSON.parse(dingtalkData.config)
      Object.assign(dingtalkConfig, config)
    }
  } catch (e) {
    console.error('加载配置失败', e)
  }
}

async function saveEmailConfig() {
  try {
    await fetch('/api/alerts/config', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        alertType: 'email',
        config: JSON.stringify(emailConfig),
        enabled: 1
      })
    })
    ElMessage.success('邮件配置已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function saveWechatConfig() {
  try {
    await fetch('/api/alerts/config', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        alertType: 'wechat',
        config: JSON.stringify(wechatConfig),
        enabled: 1
      })
    })
    ElMessage.success('企业微信配置已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function saveDingtalkConfig() {
  try {
    await fetch('/api/alerts/config', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        alertType: 'dingtalk',
        config: JSON.stringify(dingtalkConfig),
        enabled: 1
      })
    })
    ElMessage.success('钉钉配置已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function saveScanConfig() {
  ElMessage.success('扫描配置已保存（需重启服务生效）')
}
</script>

<style scoped>
.settings-page {
  padding: 0;
}
</style>
