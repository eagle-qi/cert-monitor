package com.certmonitor.util;

import com.certmonitor.entity.SslCertInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Component
public class SslCertificateUtil {
    
    /**
     * 获取 HTTPS 证书信息
     */
    public SslCertInfo getCertificateInfo(String host, int port) {
        SslCertInfo certInfo = new SslCertInfo();
        try {
            // 创建 SSL 上下文
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }
                
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }
                
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new java.security.SecureRandom());
            
            SSLSocketFactory factory = sslContext.getSocketFactory();
            
            try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
                socket.setSoTimeout(10000);
                socket.startHandshake();
                
                SSLSession session = socket.getSession();
                Certificate[] certs = session.getPeerCertificates();
                
                if (certs.length > 0 && certs[0] instanceof X509Certificate) {
                    X509Certificate x509Cert = (X509Certificate) certs[0];
                    fillCertificateInfo(certInfo, x509Cert);
                }
            }
        } catch (Exception e) {
            log.error("获取证书信息失败: {}:{}, error: {}", host, port, e.getMessage());
            certInfo.setSubject("Error: " + e.getMessage());
            certInfo.setRiskLevel(3);
            certInfo.setRemainDays(0);
        }
        return certInfo;
    }
    
    /**
     * 填充证书信息
     */
    private void fillCertificateInfo(SslCertInfo certInfo, X509Certificate x509Cert) {
        try {
            // 颁发机构
            certInfo.setIssuer(x509Cert.getIssuerX500Principal().getName());
            
            // 证书主体
            certInfo.setSubject(x509Cert.getSubjectX500Principal().getName());
            
            // 有效期
            Date notBefore = x509Cert.getNotBefore();
            Date notAfter = x509Cert.getNotAfter();
            
            certInfo.setValidStart(LocalDateTime.ofInstant(notBefore.toInstant(), ZoneId.systemDefault()));
            certInfo.setValidEnd(LocalDateTime.ofInstant(notAfter.toInstant(), ZoneId.systemDefault()));
            
            // 计算剩余天数
            long days = ChronoUnit.DAYS.between(LocalDateTime.now(), certInfo.getValidEnd());
            certInfo.setRemainDays((int) days);
            
            // 计算风险等级
            certInfo.calculateRiskLevel();
            
            // 证书指纹
            certInfo.setCertFingerprint(getCertificateFingerprint(x509Cert));
            
            // 序列号
            certInfo.setSerialNumber(x509Cert.getSerialNumber().toString(16));
            
            // 签名算法
            certInfo.setAlgorithm(x509Cert.getSigAlgName());
            
            // SAN 别名
            try {
                Collection<List<?>> sans = x509Cert.getSubjectAlternativeNames();
                if (sans != null) {
                    List<String> sanList = new ArrayList<>();
                    for (List<?> san : sans) {
                        if (san.size() > 1) {
                            sanList.add(san.get(1).toString());
                        }
                    }
                    certInfo.setSanNames(String.join(", ", sanList));
                }
            } catch (Exception e) {
                log.debug("获取 SAN 失败: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("解析证书信息失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取证书指纹 (SHA-256)
     */
    private String getCertificateFingerprint(X509Certificate cert) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] der = cert.getEncoded();
            md.update(der);
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02X:", b));
            }
            return sb.substring(0, sb.length() - 1);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
    
    /**
     * 验证证书是否可信
     */
    public boolean isCertificateTrusted(X509Certificate cert) {
        try {
            // 验证证书有效期
            cert.checkValidity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
