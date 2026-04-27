package com.certmonitor.service;

import com.certmonitor.entity.DomainAsset;
import com.certmonitor.repository.DomainAssetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AssetService {
    
    @Autowired
    private DomainAssetRepository assetRepository;
    
    public Page<DomainAsset> list(Pageable pageable, String businessGroup, Integer status) {
        if (businessGroup != null && status != null) {
            return assetRepository.findByStatusAndIsWhitelist(status, 1, pageable);
        } else if (businessGroup != null) {
            return assetRepository.findByBusinessGroup(businessGroup, pageable);
        } else if (status != null) {
            return assetRepository.findByStatus(status, pageable);
        }
        return assetRepository.findAll(pageable);
    }
    
    public List<DomainAsset> listEnabled() {
        return assetRepository.findByStatus(1);
    }
    
    public DomainAsset getById(Long id) {
        return assetRepository.findById(id).orElse(null);
    }
    
    @Transactional
    public DomainAsset save(DomainAsset asset) {
        if (asset.getDomain() == null && asset.getUrl() != null) {
            asset.setDomain(extractDomain(asset.getUrl()));
        }
        return assetRepository.save(asset);
    }
    
    @Transactional
    public DomainAsset update(Long id, DomainAsset asset) {
        DomainAsset existing = assetRepository.findById(id).orElse(null);
        if (existing == null) return null;
        
        existing.setUrl(asset.getUrl());
        existing.setProtocol(asset.getProtocol());
        existing.setBusinessGroup(asset.getBusinessGroup());
        existing.setOwner(asset.getOwner());
        existing.setTags(asset.getTags());
        existing.setStatus(asset.getStatus());
        existing.setIsWhitelist(asset.getIsWhitelist());
        existing.setDomain(extractDomain(asset.getUrl()));
        
        return assetRepository.save(existing);
    }
    
    @Transactional
    public boolean delete(Long id) {
        if (assetRepository.existsById(id)) {
            assetRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    @Transactional
    public List<DomainAsset> batchSave(List<DomainAsset> assets) {
        return assets.stream().map(asset -> {
            if (asset.getDomain() == null && asset.getUrl() != null) {
                asset.setDomain(extractDomain(asset.getUrl()));
            }
            asset.setStatus(1);
            asset.setIsWhitelist(1);
            return asset;
        }).collect(Collectors.toList());
    }
    
    public List<String> getBusinessGroups() {
        return assetRepository.findDistinctBusinessGroups();
    }
    
    public long countEnabled() {
        return assetRepository.countByStatus(1);
    }
    
    private String extractDomain(String url) {
        if (url == null) return null;
        try {
            String temp = url.replaceAll("^https?://", "").split("/")[0];
            return temp.split(":")[0];
        } catch (Exception e) {
            return url;
        }
    }
}
