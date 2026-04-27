package com.certmonitor.service;

import com.certmonitor.entity.*;
import com.certmonitor.repository.*;
import com.certmonitor.util.UrlScanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ScanService {

    @Autowired
    private ScanResultRepository scanResultRepository;
    @Autowired
    private ScanTaskRepository scanTaskRepository;
    @Autowired
    private DomainAssetRepository domainAssetRepository;
    @Autowired
    private UrlScanUtil urlScanUtil;
    @Autowired
    private AlertService alertService;

    public Page<ScanResult> getScanResults(Long assetId, Pageable pageable) {
        if (assetId != null) {
            return scanResultRepository.findByAssetIdOrderByScanTimeDesc(assetId, pageable);
        }
        return scanResultRepository.findAll(PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "scanTime")));
    }

    @Transactional
    public Map<String, Object> scanAsset(Long assetId) {
        DomainAsset asset = domainAssetRepository.findById(assetId).orElse(null);
        if (asset == null) throw new RuntimeException("资产不存在");
        ScanResult result = urlScanUtil.scanUrl(asset.getUrl());
        result.setAssetId(asset.getId());
        result.setScanTime(LocalDateTime.now());
        scanResultRepository.save(result);
        if (result.getIsAccessible() == 0) {
            alertService.triggerUrlAlert(result, asset);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("result", result);
        response.put("asset", asset);
        return response;
    }

    public void scanAllEnabled() {
        List<DomainAsset> assets = domainAssetRepository.findByStatus(1);
        for (DomainAsset asset : assets) {
            try {
                scanAsset(asset.getId());
            } catch (Exception e) {
                // continue scanning other assets
            }
        }
    }

    public Map<String, Object> getScanStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", scanResultRepository.count());
        stats.put("accessible", scanResultRepository.countByIsAccessible(1));
        stats.put("unaccessible", scanResultRepository.countByIsAccessible(0));
        return stats;
    }

    @Transactional
    public ScanTask createTask(ScanTask task) {
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        return scanTaskRepository.save(task);
    }

    public List<ScanTask> getTasks() {
        return scanTaskRepository.findAll();
    }

    @Transactional
    public void toggleTask(Long id) {
        ScanTask task = scanTaskRepository.findById(id).orElse(null);
        if (task != null) {
            task.setStatus(task.getStatus() == 1 ? 0 : 1);
            task.setUpdateTime(LocalDateTime.now());
            scanTaskRepository.save(task);
        }
    }

    @Transactional
    public void deleteTask(Long id) {
        scanTaskRepository.deleteById(id);
    }
}
