package com.evops.exchange.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 客户自助下单平台对接配置（application.yml 中 exchange.* ）。
 * 默认 endpoint 指向本应用内置 mock 回执端点，开箱即可演示全链路；
 * 正式对接时把 endpoint 换成客户平台真实地址即可。
 */
@Component
@ConfigurationProperties(prefix = "exchange")
public class ExchangeProperties {

    private final Platform platform = new Platform();
    private final Archive archive = new Archive();
    private final Retry retry = new Retry();

    public Platform getPlatform() { return platform; }
    public Archive getArchive() { return archive; }
    public Retry getRetry() { return retry; }

    public static class Platform {
        /** 客户平台付印数据接收地址；默认走内置 mock，演示不用改配置 */
        private String endpoint = "http://localhost:8080/mock/platform/orders";
        /** 连接超时 ms */
        private int connectTimeoutMs = 5000;
        /** 读取超时 ms */
        private int readTimeoutMs = 10000;

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    }

    public static class Archive {
        /** 报文文件归档根目录，按印单号建子目录 */
        private String dir = "data/exchange-archive";

        public String getDir() { return dir; }
        public void setDir(String dir) { this.dir = dir; }
    }

    public static class Retry {
        /** 最大重试次数，超过转 DEAD 人工处理 */
        private int maxRetry = 5;
        /** 退避基数秒：第 n 次重试间隔 base × 2^(n-1)，上限 30 分钟 */
        private long baseDelaySeconds = 30;
        /** 定时扫描间隔毫秒（首次启动延迟同值） */
        private long scanIntervalMs = 30000;
        /** 单次扫描最多捞多少条到期报文 */
        private int scanLimit = 100;

        public int getMaxRetry() { return maxRetry; }
        public void setMaxRetry(int maxRetry) { this.maxRetry = maxRetry; }
        public long getBaseDelaySeconds() { return baseDelaySeconds; }
        public void setBaseDelaySeconds(long baseDelaySeconds) { this.baseDelaySeconds = baseDelaySeconds; }
        public long getScanIntervalMs() { return scanIntervalMs; }
        public void setScanIntervalMs(long scanIntervalMs) { this.scanIntervalMs = scanIntervalMs; }
        public int getScanLimit() { return scanLimit; }
        public void setScanLimit(int scanLimit) { this.scanLimit = scanLimit; }
    }
}
