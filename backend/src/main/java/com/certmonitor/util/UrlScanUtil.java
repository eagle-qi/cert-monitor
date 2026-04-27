package com.certmonitor.util;

import com.certmonitor.entity.ScanResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class UrlScanUtil {
    
    private static final Map<String, Long> lastScanTime = new ConcurrentHashMap<>();
    private static final int MIN_SCAN_INTERVAL = 1000; // 最小扫描间隔 1秒
    
    static {
        // 禁用 SSL 验证（用于证书扫描）
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            log.warn("SSL 验证配置失败: {}", e.getMessage());
        }
    }
    
    /**
     * 扫描 URL 存活状态
     */
    public ScanResult scanUrl(String urlStr, int timeout) {
        ScanResult result = new ScanResult();
        long startTime = System.currentTimeMillis();
        
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            String protocol = url.getProtocol();
            
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                result.setIsAccessible(0);
                result.setErrorMessage("不支持的协议: " + protocol);
                result.setResponseTime((int) (System.currentTimeMillis() - startTime));
                return result;
            }
            
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "CertMonitor/1.0");
            connection.setRequestProperty("Connection", "close");
            
            int responseCode = connection.getResponseCode();
            int responseTime = (int) (System.currentTimeMillis() - startTime);
            
            result.setStatusCode(responseCode);
            result.setResponseTime(responseTime);
            result.setIsAccessible(responseCode >= 200 && responseCode < 400 ? 1 : 0);
            
            if (responseCode >= 400) {
                result.setErrorMessage("HTTP " + responseCode);
            }
            
        } catch (java.net.SocketTimeoutException e) {
            result.setIsAccessible(0);
            result.setErrorMessage("连接超时");
            result.setResponseTime((int) (System.currentTimeMillis() - startTime));
        } catch (java.net.ConnectException e) {
            result.setIsAccessible(0);
            result.setErrorMessage("连接被拒绝");
            result.setResponseTime((int) (System.currentTimeMillis() - startTime));
        } catch (java.net.UnknownHostException e) {
            result.setIsAccessible(0);
            result.setErrorMessage("DNS 解析失败");
            result.setResponseTime((int) (System.currentTimeMillis() - startTime));
        } catch (IOException e) {
            result.setIsAccessible(0);
            result.setErrorMessage("IO错误: " + e.getMessage());
            result.setResponseTime((int) (System.currentTimeMillis() - startTime));
            log.debug("扫描失败: {}, error: {}", urlStr, e.getMessage());
        } catch (Exception e) {
            result.setIsAccessible(0);
            result.setErrorMessage("扫描失败: " + e.getMessage());
            result.setResponseTime((int) (System.currentTimeMillis() - startTime));
            log.error("扫描 URL 失败: {}, error: {}", urlStr, e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        
        return result;
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
