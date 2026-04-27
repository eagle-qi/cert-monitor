package com.certmonitor.controller;

import com.certmonitor.entity.AlertConfig;
import com.certmonitor.entity.AlertRecord;
import com.certmonitor.service.AlertService;
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
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertController {
    
    @Autowired
    private AlertService alertService;
    
    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer isRead) {
        
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "sendTime"));
        Page<AlertRecord> alerts = alertService.list(pageable, isRead);
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", alerts.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("list", alerts.getContent());
        result.put("unread", alertService.getUnreadCount());
        
        return result;
    }
    
    @GetMapping("/unread-count")
    public Map<String, Object> getUnreadCount() {
        Map<String, Object> result = new HashMap<>();
        result.put("count", alertService.getUnreadCount());
        return result;
    }
    
    @PutMapping("/{id}/read")
    public Map<String, Object> markAsRead(@PathVariable Long id) {
        alertService.markAsRead(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }
    
    @PutMapping("/read-all")
    public Map<String, Object> markAllAsRead() {
        alertService.markAllAsRead();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }
    
    // 告警配置接口
    @GetMapping("/config")
    public List<AlertConfig> getConfigs() {
        return alertService.getConfigs();
    }
    
    @GetMapping("/config/{type}")
    public AlertConfig getConfig(@PathVariable String type) {
        return alertService.getConfigByType(type);
    }
    
    @PostMapping("/config")
    public AlertConfig saveConfig(@RequestBody AlertConfig config) {
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
