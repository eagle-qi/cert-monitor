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
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) Integer alertStatus) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "alertTime"));
        Page<AlertRecord> records = alertService.getAlertRecords(alertType, alertStatus, pageable);
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

    @PutMapping("/records/read")
    public Map<String, Object> markAsRead(@RequestBody List<Long> ids) {
        alertService.markAsRead(ids);
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

    @DeleteMapping("/records/{id}")
    public Map<String, Object> deleteRecord(@PathVariable Long id) {
        alertService.deleteAlertRecord(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    @GetMapping("/config")
    public Map<String, Object> getConfigs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<AlertConfig> configs = alertService.getAlertConfigs(page - 1, size);
        Map<String, Object> result = new HashMap<>();
        result.put("total", configs.getTotalElements());
        result.put("list", configs.getContent());
        return result;
    }

    @GetMapping("/config/{id}")
    public AlertConfig getConfig(@PathVariable Long id) {
        return alertService.getAlertConfig(id);
    }

    @PostMapping("/config")
    public AlertConfig createConfig(@RequestBody AlertConfig config) {
        return alertService.createAlertConfig(config);
    }

    @PutMapping("/config/{id}")
    public AlertConfig updateConfig(@PathVariable Long id, @RequestBody AlertConfig config) {
        return alertService.updateAlertConfig(id, config);
    }

    @DeleteMapping("/config/{id}")
    public Map<String, Object> deleteConfig(@PathVariable Long id) {
        alertService.deleteAlertConfig(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }
}
