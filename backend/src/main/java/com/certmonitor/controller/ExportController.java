package com.certmonitor.controller;

import com.certmonitor.entity.DomainAsset;
import com.certmonitor.entity.SslCertInfo;
import com.certmonitor.service.AssetService;
import com.certmonitor.service.CertService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.*;

@RestController
@RequestMapping("/api/export")
@CrossOrigin(origins = "*")
public class ExportController {

    @Autowired
    private AssetService assetService;
    @Autowired
    private CertService certService;

    @GetMapping("/assets")
    public byte[] exportAssets() throws Exception {
        List<DomainAsset> assets = assetService.listEnabled();
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("资产台账");
        Row header = sheet.createRow(0);
        String[] titles = {"ID", "URL", "协议", "域名", "业务分组", "负责人", "标签", "状态", "创建时间"};
        for (int i = 0; i < titles.length; i++) header.createCell(i).setCellValue(titles[i]);
        for (int i = 0; i < assets.size(); i++) {
            DomainAsset asset = assets.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(asset.getId());
            row.createCell(1).setCellValue(asset.getUrl());
            row.createCell(2).setCellValue(asset.getProtocol());
            row.createCell(3).setCellValue(asset.getDomain());
            row.createCell(4).setCellValue(asset.getBusinessGroup());
            row.createCell(5).setCellValue(asset.getOwner());
            row.createCell(6).setCellValue(asset.getTags());
            row.createCell(7).setCellValue(asset.getStatus() == 1 ? "启用" : "禁用");
            row.createCell(8).setCellValue(asset.getCreateTime() != null ? asset.getCreateTime().toString() : "");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    @GetMapping("/certs")
    public byte[] exportCerts() throws Exception {
        Page<SslCertInfo> certPage = certService.getCerts(null, null, PageRequest.of(0, 10000));
        List<SslCertInfo> certs = certPage.getContent();
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("证书台账");
        Row header = sheet.createRow(0);
        String[] titles = {"资产ID", "颁发机构", "证书主体", "生效时间", "过期时间", "剩余天数", "风险等级", "证书指纹", "扫描时间"};
        for (int i = 0; i < titles.length; i++) header.createCell(i).setCellValue(titles[i]);
        for (int i = 0; i < certs.size(); i++) {
            SslCertInfo cert = certs.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(cert.getAssetId());
            row.createCell(1).setCellValue(cert.getIssuer());
            row.createCell(2).setCellValue(cert.getSubject());
            row.createCell(3).setCellValue(cert.getValidStart() != null ? cert.getValidStart().toString() : "");
            row.createCell(4).setCellValue(cert.getValidEnd() != null ? cert.getValidEnd().toString() : "");
            row.createCell(5).setCellValue(cert.getRemainDays() != null ? cert.getRemainDays() : 0);
            row.createCell(6).setCellValue(getRiskLevelName(cert.getRiskLevel()));
            row.createCell(7).setCellValue(cert.getCertFingerprint());
            row.createCell(8).setCellValue(cert.getScanTime() != null ? cert.getScanTime().toString() : "");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    private String getRiskLevelName(Integer level) {
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
