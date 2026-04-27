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

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssetService {

    @Autowired
    private DomainAssetRepository domainAssetRepository;

    @Transactional
    public DomainAsset save(DomainAsset asset) {
        if (asset.getId() == null) {
            asset.setCreateTime(LocalDateTime.now());
            asset.setUpdateTime(LocalDateTime.now());
            if (asset.getStatus() == null) asset.setStatus(1);
            if (asset.getIsWhitelist() == null) asset.setIsWhitelist(1);
        } else {
            asset.setUpdateTime(LocalDateTime.now());
        }
        return domainAssetRepository.save(asset);
    }

    @Transactional
    public DomainAsset update(Long id, DomainAsset asset) {
        DomainAsset existing = domainAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资产不存在"));
        existing.setUrl(asset.getUrl());
        existing.setProtocol(asset.getProtocol());
        existing.setDomain(asset.getDomain());
        existing.setBusinessGroup(asset.getBusinessGroup());
        existing.setOwner(asset.getOwner());
        existing.setTags(asset.getTags());
        existing.setIsWhitelist(asset.getIsWhitelist());
        existing.setStatus(asset.getStatus());
        existing.setDescription(asset.getDescription());
        existing.setUpdateTime(LocalDateTime.now());
        return domainAssetRepository.save(existing);
    }

    public Page<DomainAsset> list(PageRequest pageRequest, String businessGroup, Integer status) {
        if (businessGroup != null && !businessGroup.isEmpty()) {
            return domainAssetRepository.findByBusinessGroup(businessGroup, pageRequest);
        }
        if (status != null) {
            return domainAssetRepository.findByStatus(status, pageRequest);
        }
        return domainAssetRepository.findAll(pageRequest);
    }

    public DomainAsset getById(Long id) {
        return domainAssetRepository.findById(id).orElse(null);
    }

    @Transactional
    public void delete(Long id) {
        domainAssetRepository.deleteById(id);
    }

    @Transactional
    public List<DomainAsset> batchSave(List<DomainAsset> assets) {
        for (DomainAsset asset : assets) {
            if (asset.getStatus() == null) asset.setStatus(1);
            if (asset.getIsWhitelist() == null) asset.setIsWhitelist(1);
        }
        return domainAssetRepository.saveAll(assets);
    }

    public List<DomainAsset> listEnabled() {
        return domainAssetRepository.findByStatus(1);
    }

    public List<DomainAsset> getWhitelistAssets() {
        return domainAssetRepository.findByIsWhitelist(1);
    }

    public List<String> getBusinessGroups() {
        return domainAssetRepository.findDistinctBusinessGroups();
    }

    public long countEnabled() {
        return domainAssetRepository.countByStatus(1);
    }

    public long countTotal() {
        return domainAssetRepository.count();
    }

    public long countDisabled() {
        return domainAssetRepository.countByStatus(0);
    }

    @Transactional
    public void toggleStatus(Long id) {
        DomainAsset asset = domainAssetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("资产不存在"));
        asset.setStatus(asset.getStatus() == 1 ? 0 : 1);
        asset.setUpdateTime(LocalDateTime.now());
        domainAssetRepository.save(asset);
    }
}
