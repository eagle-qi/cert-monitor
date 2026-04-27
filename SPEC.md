# URL 探测与证书过期管理系统 - 技术规范

## 1. 项目概述

### 项目名称
CertMonitor - URL 探测与 HTTPS 证书监控系统

### 核心功能
自动化 URL 存活探测 + HTTPS 证书有效期监控 + 过期分级预警 + 资产台账管理平台

### 目标用户
- 企业运维团队
- 安全运营人员
- IT 资产管理团队

## 2. 技术栈

### 后端
- **语言**: Java 17
- **框架**: Spring Boot 3.2
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **任务调度**: Quartz
- **HTTP 客户端**: Apache HttpClient
- **构建工具**: Maven

### 前端
- **框架**: Vue 3
- **UI 库**: Element Plus
- **图表**: ECharts
- **构建工具**: Vite

### 部署
- **容器化**: Docker + Docker Compose

## 3. 功能模块

### 3.1 资产台账管理
- [x] 域名/URL 批量导入
- [x] 资产分组管理
- [x] 标签管理
- [x] 黑白名单设置
- [x] 资产启用/禁用

### 3.2 URL 存活探测
- [x] HTTP/HTTPS 协议探测
- [x] 状态码检测
- [x] 响应耗时统计
- [x] 超时配置
- [x] 即时扫描
- [x] 定时扫描（Cron 表达式）

### 3.3 SSL 证书监控
- [x] 证书信息抓取（颁发机构、有效期、SAN 别名）
- [x] 剩余天数计算
- [x] 分级预警（正常/预警/高危/已过期）
- [x] 证书变更检测

### 3.4 告警通知
- [x] 邮件告警
- [x] 企业微信 Webhook
- [x] 钉钉 Webhook
- [x] 告警抑制（防轰炸）

### 3.5 可视化报表
- [x] 资产大盘总览
- [x] 证书过期趋势图
- [x] URL 异常分布图
- [x] 扫描历史日志
- [x] 数据导出（Excel）

## 4. 数据库表设计

### domain_asset (资产表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| url | VARCHAR(500) | 探测 URL |
| protocol | VARCHAR(10) | http/https |
| business_group | VARCHAR(100) | 业务分组 |
| owner | VARCHAR(100) | 负责人 |
| tags | VARCHAR(500) | 标签 |
| status | TINYINT | 状态 0禁用 1启用 |
| is_whitelist | TINYINT | 0黑名单 1白名单 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### ssl_cert_info (证书信息表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| asset_id | BIGINT | 关联资产 ID |
| issuer | VARCHAR(500) | 颁发机构 |
| subject | VARCHAR(500) | 证书主体 |
| valid_start | DATETIME | 生效时间 |
| valid_end | DATETIME | 过期时间 |
| remain_days | INT | 剩余天数 |
| risk_level | TINYINT | 0正常 1预警 2高危 3过期 |
| cert_fingerprint | VARCHAR(100) | 证书指纹 |
| san_names | TEXT | SAN 别名 |
| scan_time | DATETIME | 扫描时间 |

### scan_task (扫描任务表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| task_name | VARCHAR(100) | 任务名称 |
| cron_expression | VARCHAR(50) | Cron 表达式 |
| scan_type | TINYINT | 0即时 1定时 |
| status | TINYINT | 0停用 1启用 |
| create_time | DATETIME | 创建时间 |

### scan_result (扫描结果表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| asset_id | BIGINT | 资产 ID |
| is_accessible | TINYINT | 是否可访问 |
| status_code | INT | HTTP 状态码 |
| response_time | INT | 响应耗时(ms) |
| error_message | VARCHAR(500) | 错误信息 |
| scan_time | DATETIME | 扫描时间 |

### alert_config (告警配置表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| alert_type | VARCHAR(20) | email/wechat/dingtalk |
| config | TEXT | 配置内容 |
| enabled | TINYINT | 是否启用 |

### alert_record (告警记录表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| asset_id | BIGINT | 资产 ID |
| alert_type | VARCHAR(20) | 告警类型 |
| risk_level | TINYINT | 风险等级 |
| message | TEXT | 告警内容 |
| is_read | TINYINT | 0未读 1已读 |
| send_time | DATETIME | 发送时间 |

## 5. API 接口设计

### 资产接口
- `GET /api/assets` - 资产列表
- `POST /api/assets` - 添加资产
- `PUT /api/assets/{id}` - 更新资产
- `DELETE /api/assets/{id}` - 删除资产
- `POST /api/assets/batch` - 批量导入

### 扫描接口
- `POST /api/scan/start` - 启动扫描
- `GET /api/scan/results/{assetId}` - 扫描结果
- `GET /api/scan/history` - 扫描历史

### 证书接口
- `GET /api/certs` - 证书列表
- `GET /api/certs/expiring` - 即将过期证书
- `GET /api/certs/stats` - 证书统计

### 告警接口
- `GET /api/alerts` - 告警记录
- `PUT /api/alerts/{id}/read` - 标记已读
- `POST /api/alerts/config` - 配置告警

### 报表接口
- `GET /api/dashboard/stats` - 大盘统计
- `GET /api/dashboard/trend` - 趋势数据
- `GET /api/export/assets` - 导出资产
- `GET /api/export/certs` - 导出证书

## 6. 证书风险分级

| 等级 | 剩余天数 | 说明 |
|------|----------|------|
| 正常 (0) | > 30 天 | 绿色标识 |
| 预警 (1) | 15-30 天 | 黄色标识 |
| 高危 (2) | 1-15 天 | 橙色标识 |
| 过期 (3) | <= 0 天 | 红色标识 |

## 7. 部署架构

```
┌─────────────────────────────────────────────────┐
│                   Nginx (可选)                    │
└─────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────┐
│              Docker Compose Stack               │
│  ┌─────────────┐  ┌─────────────┐              │
│  │   Vue3 UI   │  │ Spring Boot │              │
│  │   (80)      │  │   (8080)    │              │
│  └─────────────┘  └─────────────┘              │
│                          │                      │
│  ┌─────────────┐  ┌─────────────┐              │
│  │    MySQL    │  │    Redis    │              │
│  │   (3306)    │  │   (6379)    │              │
│  └─────────────┘  └─────────────┘              │
└─────────────────────────────────────────────────┘
```

## 8. 环境要求

- JDK 17+
- Node.js 18+
- Docker & Docker Compose
- MySQL 8.0
- Redis 6.0+
