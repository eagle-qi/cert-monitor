# CertMonitor - URL 探测与 HTTPS 证书监控系统

## 项目简介

一款自动化 URL 存活探测 + HTTPS 证书有效期监控 + 过期分级预警 + 资产台账管理平台，适用于企业域名资产统一管理。

### 核心功能

- ✅ 批量 URL 可用性探测（连通性、状态码、响应耗时）
- ✅ HTTPS 证书自动解析、有效期计算、过期分级预警
- ✅ 资产分组管理、标签管理、黑白名单
- ✅ 多渠道告警（邮件、企业微信、钉钉）
- ✅ 可视化大盘、历史记录、Excel 导出

## 技术栈

| 层级 | 技术选型 |
|------|----------|
| 后端 | Java 17 + Spring Boot 3.2 |
| 前端 | Vue 3 + Element Plus + ECharts |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis |
| 部署 | Docker + Docker Compose |

## 快速启动

### 方式一：Docker Compose 一键部署（推荐）

```bash
cd cert-monitor

# 构建并启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

访问地址：
- 前端界面：http://localhost
- 后端 API：http://localhost:8080

### 方式二：本地开发

#### 后端启动

```bash
cd backend

# 创建数据库
mysql -u root -p < src/main/resources/schema.sql

# 启动服务
mvn spring-boot:run
```

#### 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

## 项目结构

```
cert-monitor/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com/certmonitor/
│   │       ├── config/         # 配置类
│   │       ├── controller/     # REST API 控制器
│   │       ├── entity/         # 实体类
│   │       ├── repository/     # JPA 数据访问层
│   │       ├── scheduler/      # 定时任务
│   │       ├── service/        # 业务逻辑层
│   │       └── util/           # 工具类
│   └── src/main/resources/
│       ├── application.yml     # 配置文件
│       └── schema.sql          # 数据库建表脚本
│
├── frontend/                   # Vue3 前端
│   ├── src/
│   │   ├── api/               # API 接口封装
│   │   ├── router/            # 路由配置
│   │   └── views/             # 页面组件
│   └── nginx.conf             # Nginx 配置
│
├── docker-compose.yml          # 容器编排
├── SPEC.md                     # 技术规范文档
└── README.md                   # 说明文档
```

## 功能模块

### 1. 监控大盘
- 资产总数、可用率统计
- 证书风险分布饼图
- 证书过期趋势图
- 即将过期证书列表

### 2. 资产管理
- 域名/URL 批量导入
- 业务分组、标签管理
- 资产启用/禁用
- 单个/批量扫描

### 3. 证书管理
- 证书信息查看（颁发机构、有效期、SAN 别名）
- 风险等级筛选
- 证书重新扫描
- Excel 导出

### 4. 告警记录
- 未读/已读状态管理
- 告警时间线展示
- 批量标记已读

### 5. 系统设置
- 邮件告警配置
- 企业微信 Webhook
- 钉钉 Webhook
- 扫描参数配置

## 证书风险分级

| 等级 | 剩余天数 | 说明 | 颜色 |
|------|----------|------|------|
| 正常 | > 30 天 | 绿色 | #67C23A |
| 预警 | 15-30 天 | 黄色 | #E6A23C |
| 高危 | 1-15 天 | 橙色 | #F56C6C |
| 已过期 | ≤ 0 天 | 红色 | #909399 |

## API 接口

### 资产接口
- `GET /api/assets` - 资产列表
- `POST /api/assets` - 添加资产
- `PUT /api/assets/{id}` - 更新资产
- `DELETE /api/assets/{id}` - 删除资产
- `POST /api/assets/batch` - 批量导入

### 扫描接口
- `POST /api/scan/start/{assetId}` - 扫描单个资产
- `POST /api/scan/start-all` - 全量扫描

### 证书接口
- `GET /api/certs` - 证书列表
- `POST /api/certs/scan/{assetId}` - 扫描证书
- `GET /api/certs/stats` - 证书统计

### 大盘接口
- `GET /api/dashboard/stats` - 大盘统计

## 告警配置

### 邮件告警配置
```json
{
  "fromEmail": "your_email@qq.com",
  "smtpHost": "smtp.qq.com",
  "smtpPort": "587",
  "password": "your_auth_code",
  "toEmail": "recipient@example.com"
}
```

### 企业微信 Webhook
```json
{
  "webhookUrl": "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx"
}
```

### 钉钉 Webhook
```json
{
  "webhookUrl": "https://oapi.dingtalk.com/robot/send?access_token=xxx",
  "secret": "可选的加签密钥"
}
```

## 定时任务

| 任务 | 表达式 | 说明 |
|------|--------|------|
| 全量扫描 | `0 0 2 * * ?` | 每天凌晨 2 点 |
| 证书增量扫描 | `0 0 * * * ?` | 每小时执行 |

## 环境变量

### 后端环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| SPRING_DATASOURCE_URL | 数据库连接 | jdbc:mysql://localhost:3306/cert_monitor |
| SPRING_DATASOURCE_USERNAME | 数据库用户名 | root |
| SPRING_DATASOURCE_PASSWORD | 数据库密码 | root123456 |
| SPRING_DATA_REDIS_HOST | Redis 主机 | localhost |
| SPRING_DATA_REDIS_PORT | Redis 端口 | 6379 |

## 常见问题

### Q: Docker 部署后无法访问？
A: 检查所有服务是否正常运行：`docker-compose ps`，查看日志：`docker-compose logs -f`

### Q: 证书扫描失败？
A: 确保目标域名可访问，且防火墙允许出站 HTTPS 连接

### Q: 邮件告警无法发送？
A: 检查 SMTP 配置，使用邮箱授权码而非登录密码

## License

MIT License
