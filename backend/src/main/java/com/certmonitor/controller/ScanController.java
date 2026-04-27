package com.certmonitor.controller;

import com.certmonitor.entity.ScanResult;
import com.certmonitor.entity.ScanTask;
import com.certmonitor.service.ScanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/scan")
@CrossOrigin(origins = "*")
public class ScanController {
    
    @Autowired
    private ScanService scanService;
    
    @GetMapping("/results")
    public Map<String, Object> results(
            @RequestParam(required = false) Long assetId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "scanTime"));
        Page<ScanResult> results = scanService.getScanResults(assetId, pageable);
        Map<String, Object> response = new HashMap<>();
        response.put("total", results.getTotalElements());
        response.put("page", page);
        response.put("size", size);
        response.put("list", results.getContent());
        return response;
    }
    
    @GetMapping("/results/{assetId}/latest")
    public ScanResult getLatestResult(@PathVariable Long assetId) {
        return scanService.getScanResults(assetId, PageRequest.of(0, 1)).getContent().stream().findFirst().orElse(null);
    }
    
    @PostMapping("/start/{assetId}")
    public Map<String, Object> startScan(@PathVariable Long assetId) {
        long start = System.currentTimeMillis();
        Map<String, Object> result = scanService.scanAsset(assetId);
        long duration = System.currentTimeMillis() - start;
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("result", result);
        response.put("duration", duration);
        return response;
    }
    
    @PostMapping("/start-all")
    public Map<String, Object> startAllScan() {
        long start = System.currentTimeMillis();
        scanService.scanAllEnabled();
        long duration = System.currentTimeMillis() - start;
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "全量扫描已启动");
        response.put("duration", duration);
        return response;
    }
    
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return scanService.getScanStats();
    }

    @GetMapping("/tasks")
    public List<ScanTask> getTasks() {
        return scanService.getTasks();
    }

    @PostMapping("/tasks")
    public ScanTask createTask(@RequestBody ScanTask task) {
        return scanService.createTask(task);
    }

    @PutMapping("/tasks/{id}/toggle")
    public Map<String, Object> toggleTask(@PathVariable Long id) {
        scanService.toggleTask(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    @DeleteMapping("/tasks/{id}")
    public Map<String, Object> deleteTask(@PathVariable Long id) {
        scanService.deleteTask(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }
}
