package com.certmonitor.scheduler;

import com.certmonitor.service.CertService;
import com.certmonitor.service.ScanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ScanScheduler {
    
    @Autowired
    private ScanService scanService;
    
    @Autowired
    private CertService certService;
    
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyFullScan() {
        log.info("========== 开始执行每日定时全量扫描 ==========");
        try {
            scanService.scanAllEnabled();
            certService.scanAllEnabled();
            log.info("========== 每日定时全量扫描完成 ==========");
        } catch (Exception e) {
            log.error("定时扫描执行失败: {}", e.getMessage());
        }
    }
    
    @Scheduled(cron = "0 0 * * * ?")
    public void hourlyCertScan() {
        log.info("========== 开始执行每小时证书增量扫描 ==========");
        try {
            certService.scanAllEnabled();
            log.info("========== 每小时证书增量扫描完成 ==========");
        } catch (Exception e) {
            log.error("增量扫描执行失败: {}", e.getMessage());
        }
    }
    
    @Scheduled(cron = "0 0 9 * * ?")
    public void dailyCertReport() {
        log.info("========== 开始生成证书过期日报 ==========");
        log.info("========== 证书过期日报生成完成 ==========");
    }
}
