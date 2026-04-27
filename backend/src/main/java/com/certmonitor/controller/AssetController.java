package com.certmonitor.controller;

import com.certmonitor.entity.DomainAsset;
import com.certmonitor.service.AssetService;
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
@RequestMapping("/api/assets")
@CrossOrigin(origins = "*")
public class AssetController {
    
    @Autowired
    private AssetService assetService;
    
    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String businessGroup,
            @RequestParam(required = false) Integer status) {
        
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<DomainAsset> assets = assetService.list(pageable, businessGroup, status);
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", assets.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("list", assets.getContent());
        
        return result;
    }
    
    @GetMapping("/{id}")
    public DomainAsset getById(@PathVariable Long id) {
        return assetService.getById(id);
    }
    
    @PostMapping
    public DomainAsset create(@RequestBody DomainAsset asset) {
        return assetService.save(asset);
    }
    
    @PutMapping("/{id}")
    public DomainAsset update(@PathVariable Long id, @RequestBody DomainAsset asset) {
        return assetService.update(id, asset);
    }
    
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        boolean success = assetService.delete(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        return result;
    }
    
    @PostMapping("/batch")
    public Map<String, Object> batchImport(@RequestBody List<DomainAsset> assets) {
        List<DomainAsset> saved = assetService.batchSave(assets);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", saved.size());
        result.put("list", saved);
        return result;
    }
    
    @GetMapping("/groups")
    public List<String> getBusinessGroups() {
        return assetService.getBusinessGroups();
    }
    
    @GetMapping("/count")
    public Map<String, Object> count() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", assetService.countEnabled());
        return result;
    }
}
