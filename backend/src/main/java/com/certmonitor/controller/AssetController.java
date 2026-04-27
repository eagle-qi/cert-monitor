package com.certmonitor.controller;

import com.certmonitor.entity.DomainAsset;
import com.certmonitor.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
@CrossOrigin(origins = "*")
public class AssetController {

    @Autowired
    private AssetService assetService;

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String businessGroup,
            @RequestParam(required = false) Integer status) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<DomainAsset> assets = assetService.list(pageRequest, businessGroup, status);
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
        assetService.delete(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    @PostMapping("/batch")
    public Map<String, Object> batchCreate(@RequestBody List<DomainAsset> assets) {
        assetService.batchSave(assets);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", assets.size());
        return result;
    }

    @PutMapping("/{id}/toggle")
    public Map<String, Object> toggleStatus(@PathVariable Long id) {
        assetService.toggleStatus(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    @GetMapping("/groups")
    public List<String> getBusinessGroups() {
        return assetService.getBusinessGroups();
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", assetService.countTotal());
        stats.put("enabled", assetService.countEnabled());
        stats.put("disabled", assetService.countDisabled());
        return stats;
    }
}
