-- 创建数据库
CREATE DATABASE IF NOT EXISTS cert_monitor DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE cert_monitor;

-- 资产表
CREATE TABLE IF NOT EXISTS domain_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url VARCHAR(500) NOT NULL COMMENT '探测URL',
    protocol VARCHAR(10) DEFAULT 'https' COMMENT 'http/https',
    domain VARCHAR(255) COMMENT '域名',
    business_group VARCHAR(100) COMMENT '业务分组',
    owner VARCHAR(100) COMMENT '负责人',
    tags VARCHAR(500) COMMENT '标签，逗号分隔',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    is_whitelist TINYINT DEFAULT 1 COMMENT '0黑名单 1白名单',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_url (url(255)),
    INDEX idx_status (status),
    INDEX idx_business_group (business_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产域名表';

-- 证书信息表
CREATE TABLE IF NOT EXISTS ssl_cert_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL COMMENT '关联资产ID',
    issuer VARCHAR(500) COMMENT '颁发机构',
    subject VARCHAR(500) COMMENT '证书主体',
    valid_start DATETIME COMMENT '生效时间',
    valid_end DATETIME COMMENT '过期时间',
    remain_days INT DEFAULT 0 COMMENT '剩余天数',
    risk_level TINYINT DEFAULT 0 COMMENT '0正常 1预警 2高危 3过期',
    cert_fingerprint VARCHAR(100) COMMENT '证书指纹',
    san_names TEXT COMMENT 'SAN别名',
    serial_number VARCHAR(100) COMMENT '证书序列号',
    algorithm VARCHAR(50) COMMENT '签名算法',
    scan_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_asset_id (asset_id),
    INDEX idx_risk_level (risk_level),
    INDEX idx_valid_end (valid_end),
    FOREIGN KEY (asset_id) REFERENCES domain_asset(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SSL证书信息表';

-- 扫描任务表
CREATE TABLE IF NOT EXISTS scan_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    cron_expression VARCHAR(50) COMMENT 'Cron表达式',
    scan_type TINYINT DEFAULT 0 COMMENT '0即时 1定时',
    status TINYINT DEFAULT 0 COMMENT '0停用 1启用',
    description VARCHAR(500) COMMENT '任务描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扫描任务表';

-- 扫描结果表
CREATE TABLE IF NOT EXISTS scan_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL COMMENT '资产ID',
    is_accessible TINYINT DEFAULT 0 COMMENT '是否可访问',
    status_code INT COMMENT 'HTTP状态码',
    response_time INT COMMENT '响应耗时ms',
    error_message VARCHAR(500) COMMENT '错误信息',
    scan_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_asset_id (asset_id),
    INDEX idx_scan_time (scan_time),
    FOREIGN KEY (asset_id) REFERENCES domain_asset(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扫描结果表';

-- 告警配置表
CREATE TABLE IF NOT EXISTS alert_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_type VARCHAR(20) NOT NULL COMMENT 'email/wechat/dingtalk',
    config TEXT COMMENT '配置内容JSON',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (alert_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警配置表';

-- 告警记录表
CREATE TABLE IF NOT EXISTS alert_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT COMMENT '资产ID',
    alert_type VARCHAR(20) COMMENT '告警类型 cert/scan',
    risk_level TINYINT COMMENT '风险等级',
    title VARCHAR(200) COMMENT '告警标题',
    message TEXT COMMENT '告警内容',
    is_read TINYINT DEFAULT 0 COMMENT '0未读 1已读',
    send_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_asset_id (asset_id),
    INDEX idx_is_read (is_read),
    INDEX idx_send_time (send_time),
    FOREIGN KEY (asset_id) REFERENCES domain_asset(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';
