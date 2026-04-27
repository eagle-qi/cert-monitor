package com.certmonitor.controller;

import com.certmonitor.entity.AlertConfig;
import com.certmonitor.entity.AlertRecord;
import com.certmonitor.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @GetMapping("/records")
    public Map<String, Object> listRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer isRead) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "sendTime"));
        Page<AlertRecord> records = alertService.list(pageable, isRead);
        Map<String, Object> result = new HashMap<>();
        result.put("total", records.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("list", records.getContent());
        return result;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return alertService.getAlertStats();
    }

    @PutMapping("/records/read/{id}")
    public Map<String, Object> markAsRead(@PathVariable Long id) {
        alertService.markAsRead(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    @PutMapping("/records/read-all")
    public Map<String, Object> markAllAsRead() {
        alertService.markAllAsRead();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    @GetMapping("/config")
    public List<AlertConfig> getConfigs() {
        return alertService.getConfigs();
    }

    @GetMapping("/config/{id}")
    public AlertConfig getConfig(@PathVariable Long id) {
        return alertService.getConfigByType(null);
    }

    @PostMapping("/config")
    public AlertConfig createConfig(@RequestBody AlertConfig config) {
        return alertService.saveConfig(config);
    }

    @DeleteMapping("/config/{id}")
    public Map<String, Object> deleteConfig(@PathVariable Long id) {
        alertService.deleteConfig(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }
}
