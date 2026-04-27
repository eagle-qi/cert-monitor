package com.certmonitor.service;

import com.certmonitor.entity.DomainAsset;
import com.certmonitor.repository.DomainAssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssetService {

    @Autowired
    private DomainAssetRepository domainAssetRepository;

    @Transactional
    public DomainAsset createAsset(DomainAsset asset) {
        if (asset.getStatus() == null) {
            asset.setStatus(1); // 默认启用
        }
        if (asset.getCreateTime() == null) {
            asset.setCreateTime(new java.util.Date());
        }
        return domainAssetRepository.save(asset);
    }

    @Transactional
    public DomainAsset updateAsset(Long id, DomainAsset asset) {
        DomainAsset existing = domainAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资产不存在"));
        existing.setUrl(asset.getUrl());
        existing.setProtocol(asset.getProtocol());
        existing.setBusinessGroup(asset.getBusinessGroup());
        existing.setOwner(asset.getOwner());
        existing.setTags(asset.getTags());
        existing.setIsWhitelist(asset.getIsWhitelist());
        existing.setStatus(asset.getStatus());
        existing.setDescription(asset.getDescription());
        return domainAssetRepository.save(existing);
    }

    public Page<DomainAsset> getAssets(String businessGroup, Integer status, String search, Pageable pageable) {
        if (businessGroup != null && !businessGroup.isEmpty()) {
            return domainAssetRepository.findByBusinessGroup(businessGroup, pageable);
        }
        if (status != null) {
            return domainAssetRepository.findByStatusAndIsWhitelist(status, 0, pageable);
        }
        if (search != null && !search.isEmpty()) {
            return domainAssetRepository.findByUrlContaining(search, pageable);
        }
        return domainAssetRepository.findAll(pageable);
    }

    public DomainAsset getAsset(Long id) {
        return domainAssetRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deleteAsset(Long id) {
        domainAssetRepository.deleteById(id);
    }

    @Transactional
    public void batchCreateAssets(List<DomainAsset> assets) {
        for (DomainAsset asset : assets) {
            if (asset.getStatus() == null) {
                asset.setStatus(1);
            }
            if (asset.getCreateTime() == null) {
                asset.setCreateTime(new java.util.Date());
            }
        }
        domainAssetRepository.saveAll(assets);
    }

    public List<DomainAsset> getEnabledAssets() {
        return domainAssetRepository.findByStatus(1);
    }

    public List<DomainAsset> getWhitelistAssets() {
        return domainAssetRepository.findByIsWhitelist(1);
    }

    public List<String> getBusinessGroups() {
        return domainAssetRepository.findDistinctBusinessGroups();
    }

    public long getTotalCount() {
        return domainAssetRepository.count();
    }

    public long getEnabledCount() {
        return domainAssetRepository.countByStatus(1);
    }

    public long getDisabledCount() {
        return domainAssetRepository.countByStatus(0);
    }

    public long getWhitelistCount() {
        return domainAssetRepository.countWhitelistAssets();
    }

    @Transactional
    public void toggleStatus(Long id) {
        DomainAsset asset = domainAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资产不存在"));
        asset.setStatus(asset.getStatus() == 1 ? 0 : 1);
        domainAssetRepository.save(asset);
    }
}
