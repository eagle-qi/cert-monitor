package com.certmonitor.util;

import com.certmonitor.entity.ScanResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classics.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classics.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class UrlScanUtil {
    
    private static final Map<String, Object> httpClients = new ConcurrentHashMap<>();
    private static final int DEFAULT_TIMEOUT = 5000;
    
    /**
     * 获取 HttpClient
     */
    private static Object getHttpClient(String key, int timeout) {
        return httpClients.computeIfAbsent(key, k -> {
            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
            cm.setMaxTotal(100);
            cm.setDefaultMaxPerRoute(20);
            
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(timeout))
                    .setResponseTimeout(Timeout.ofMilliseconds(timeout))
                    .build();
            
            return HttpClients.custom()
                    .setConnectionManager(cm)
                    .setDefaultRequestConfig(requestConfig)
                    .build();
        });
    }
    
    /**
     * 扫描 URL 存活状态
     */
    public ScanResult scanUrl(String urlStr, int timeout) {
        ScanResult result = new ScanResult();
        long startTime = System.currentTimeMillis();
        
        try {
            URL url = new URL(urlStr);
            String protocol = url.getProtocol();
            String host = url.getHost();
            int port = url.getPort() != -1 ? url.getPort() : 
                      (protocol.equals("https") ? 443 : 80);
            
            if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
                return scanHttpUrl(urlStr, timeout, startTime);
            } else {
                result.setIsAccessible(0);
                result.setErrorMessage("不支持的协议: " + protocol);
            }
        } catch (Exception e) {
            result.setIsAccessible(0);
            result.setErrorMessage("扫描失败: " + e.getMessage());
            log.error("扫描 URL 失败: {}, error: {}", urlStr, e.getMessage());
        }
        
        result.setResponseTime((int) (System.currentTimeMillis() - startTime));
        return result;
    }
    
    /**
     * 扫描 HTTP/HTTPS URL
     */
    private ScanResult scanHttpUrl(String urlStr, int timeout, long startTime) {
        ScanResult result = new ScanResult();
        
        try {
            org.apache.hc.client5.http.classics.methods.HttpGet request = 
                    new HttpGet(urlStr);
            request.setHeader("User-Agent", "CertMonitor/1.0");
            request.setHeader("Connection", "close");
            
            var httpClient = getHttpClient("client_" + timeout, timeout);
            var response = ((org.apache.hc.client5.http.impl.classics.CloseableHttpClient) httpClient)
                    .execute(request);
            
            int statusCode = response.getCode();
            long responseTime = System.currentTimeMillis() - startTime;
            
            result.setStatusCode(statusCode);
            result.setResponseTime((int) responseTime);
            result.setIsAccessible(statusCode >= 200 && statusCode < 400 ? 1 : 0);
            
            if (statusCode >= 400) {
                result.setErrorMessage("HTTP " + statusCode);
            }
            
            response.close();
            
        } catch (org.apache.hc.client5.http.ClientProtocolException e) {
            result.setIsAccessible(0);
            result.setErrorMessage("协议错误: " + e.getMessage());
            result.setResponseTime((int) (System.currentTimeMillis() - startTime));
        } catch (org.apache.hc.core5.io.CloseIgnoredException e) {
            // 忽略关闭异常
            result.setIsAccessible(1);
            result.setResponseTime((int) (System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            result.setIsAccessible(0);
            result.setErrorMessage("连接失败: " + getErrorType(e));
            result.setResponseTime((int) (System.currentTimeMillis() - startTime));
            log.debug("扫描失败: {}, error: {}", urlStr, e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取错误类型
     */
    private String getErrorType(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return "未知错误";
        
        if (msg.contains("ConnectTimeoutException") || msg.contains("timeout")) {
            return "连接超时";
        } else if (msg.contains("ConnectException")) {
            return "连接被拒绝";
        } else if (msg.contains("UnknownHostException")) {
            return "DNS 解析失败";
        } else if (msg.contains("SSLHandshakeException")) {
            return "SSL 握手失败";
        } else if (msg.contains("SocketTimeoutException")) {
            return "读取超时";
        }
        return msg.length() > 100 ? msg.substring(0, 100) : msg;
    }
    
    /**
     * 提取域名
     */
    public String extractDomain(String url) {
        try {
            URL parsedUrl = new URL(url);
            String host = parsedUrl.getHost();
            int port = parsedUrl.getPort();
            if (port != -1) {
                return host + ":" + port;
            }
            return host;
        } catch (Exception e) {
            return url;
        }
    }
}
