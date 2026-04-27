package com.certmonitor.controller;

import com.alibaba.excel.EasyExcel;
import com.certmonitor.entity.DomainAsset;
import com.certmonitor.entity.SslCertInfo;
import com.certmonitor.service.AssetService;
import com.certmonitor.service.CertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/export")
@CrossOrigin(origins = "*")
public class ExportController {
    
    @Autowired
    private AssetService assetService;
    
    @Autowired
    private CertService certService;
    
    /**
     * 导出资产列表 Excel
     */
    @GetMapping("/assets")
    public void exportAssets(HttpServletResponse response) throws IOException {
        List<DomainAsset> assets = assetService.listEnabled();
        
        String filename = URLEncoder.encode("资产台账", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + filename + ".xlsx");
        
        EasyExcel.write(response.getOutputStream(), DomainAssetExcelVO.class)
                .sheet("资产列表")
                .doWrite(assets.stream().map(DomainAssetExcelVO::fromEntity).toList());
    }
    
    /**
     * 导出证书列表 Excel
     */
    @GetMapping("/certs")
    public void exportCerts(HttpServletResponse response) throws IOException {
        List<SslCertInfo> certs = certService.getExpiringCerts(365);
        
        String filename = URLEncoder.encode("证书过期清单", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + filename + ".xlsx");
        
        EasyExcel.write(response.getOutputStream(), SslCertExcelVO.class)
                .sheet("证书列表")
                .doWrite(certs.stream().map(SslCertExcelVO::fromEntity).toList());
    }
    
    // Excel VO 类
    @lombok.Data
    public static class DomainAssetExcelVO {
        private String url;
        private String domain;
        private String businessGroup;
        private String owner;
        private String tags;
        private String status;
        
        public static DomainAssetExcelVO fromEntity(DomainAsset asset) {
            DomainAssetExcelVO vo = new DomainAssetExcelVO();
            vo.setUrl(asset.getUrl());
            vo.setDomain(asset.getDomain());
            vo.setBusinessGroup(asset.getBusinessGroup());
            vo.setOwner(asset.getOwner());
            vo.setTags(asset.getTags());
            vo.setStatus(asset.getStatus() == 1 ? "启用" : "禁用");
            return vo;
        }
    }
    
    @lombok.Data
    public static class SslCertExcelVO {
        private String url;
        private String issuer;
        private String subject;
        private String validStart;
        private String validEnd;
        private Integer remainDays;
        private String riskLevel;
        private String scanTime;
        
        public static SslCertExcelVO fromEntity(SslCertInfo cert) {
            SslCertExcelVO vo = new SslCertExcelVO();
            vo.setIssuer(cert.getIssuer());
            vo.setSubject(cert.getSubject());
            vo.setValidStart(cert.getValidStart() != null ? cert.getValidStart().toString() : "");
            vo.setValidEnd(cert.getValidEnd() != null ? cert.getValidEnd().toString() : "");
            vo.setRemainDays(cert.getRemainDays());
            vo.setRiskLevel(getRiskLevelText(cert.getRiskLevel()));
            vo.setScanTime(cert.getScanTime() != null ? cert.getScanTime().toString() : "");
            return vo;
        }
        
        private static String getRiskLevelText(Integer level) {
            if (level == null) return "未知";
            switch (level) {
                case 0: return "正常";
                case 1: return "预警";
                case 2: return "高危";
                case 3: return "已过期";
                default: return "未知";
            }
        }
    }
}
