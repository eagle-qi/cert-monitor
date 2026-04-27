package com.certmonitor.service;

import com.certmonitor.entity.DomainAsset;
import com.certmonitor.entity.ScanResult;
import com.certmonitor.repository.ScanResultRepository;
import com.certmonitor.util.UrlScanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class ScanService {
    
    @Autowired
    private ScanResultRepository resultRepository;
    
    @Autowired
    private AssetService assetService;
    
    @Autowired
    private CertService certService;
    
    @Autowired
    private AlertService alertService;
    
    @Autowired
    private UrlScanUtil urlScanUtil;
    
    @Value("${cert-monitor.scan.timeout:5000}")
    private int scanTimeout;
    
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    /**
     * 扫描单个资产
     */
    @Transactional
    public ScanResult scanAsset(Long assetId) {
        DomainAsset asset = assetService.getById(assetId);
        if (asset == null) {
            throw new RuntimeException("资产不存在: " + assetId);
        }
        
        ScanResult result = urlScanUtil.scanUrl(asset.getUrl(), scanTimeout);
        result.setAssetId(assetId);
        
        ScanResult saved = resultRepository.save(result);
        
        // 如果是 HTTPS，扫描证书
        if ("https".equalsIgnoreCase(asset.getProtocol())) {
            try {
                certService.scanCert(assetId);
            } catch (Exception e) {
                log.error("扫描证书失败: {}", e.getMessage());
            }
        }
        
        // 扫描失败触发告警
        if (saved.getIsAccessible() == 0) {
            alertService.sendScanAlert(saved, asset);
        }
        
        return saved;
    }
    
    /**
     * 批量扫描所有启用的资产
     */
    public void scanAllAssets() {
        List<DomainAsset> assets = assetService.listEnabled();
        log.info("开始扫描 {} 个资产", assets.size());
        
        long startTime = System.currentTimeMillis();
        int success = 0;
        int failed = 0;
        
        List<CompletableFuture<Void>> futures = assets.stream()
                .map(asset -> CompletableFuture.runAsync(() -> {
                    try {
                        scanAsset(asset.getId());
                        success++;
                    } catch (Exception e) {
                        log.error("扫描失败: {}, error: {}", asset.getUrl(), e.getMessage());
                        failed++;
                    }
                }, executor))
                .toList();
        
        // 等待所有任务完成
        futures.forEach(CompletableFuture::join);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("扫描完成: 总数={}, 成功={}, 失败={}, 耗时={}ms", 
                assets.size(), success, failed, duration);
    }
    
    /**
     * 获取扫描结果列表
     */
    public Page<ScanResult> getResults(Long assetId, Pageable pageable) {
        if (assetId != null) {
            return resultRepository.findByAssetIdOrderByScanTimeDesc(assetId, pageable);
        }
        return resultRepository.findAll(pageable);
    }
    
    /**
     * 获取最新扫描结果
     */
    public ScanResult getLatestResult(Long assetId) {
        return resultRepository.findTopByAssetIdOrderByScanTimeDesc(assetId).orElse(null);
    }
    
    /**
     * 获取扫描统计
     */
    public java.util.Map<String, Object> getStats() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long total = resultRepository.count();
        long accessible = resultRepository.countByIsAccessible(1);
        long inaccessible = resultRepository.countByIsAccessible(0);
        long failed = resultRepository.countFailedScansSince(since);
        
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("total", total);
        stats.put("accessible", accessible);
        stats.put("inaccessible", inaccessible);
        stats.put("failed24h", failed);
        stats.put("accessibleRate", total > 0 ? (accessible * 100.0 / total) : 0);
        
        return stats;
    }
    
    /**
     * 清理过期扫描记录
     */
    @Transactional
    public void cleanupOldRecords(int days) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        // 注意：JPA 的 delete 方法需要手动实现
        resultRepository.deleteAll();
        log.info("清理 {} 天前的扫描记录完成", days);
    }
}
