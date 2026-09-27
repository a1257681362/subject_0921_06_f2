package com.evops.exchange.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 交换模块 HTTP 与定时配置。
 * 平台调用用独立 RestTemplate（超时按 exchange.platform.* 配置）；
 * 开启 Spring 定时驱动失败重试扫描。
 */
@Configuration
@EnableScheduling
public class ExchangeHttpConfig {

    @Bean
    public RestTemplate exchangeRestTemplate(RestTemplateBuilder builder, ExchangeProperties properties) {
        return builder
                .setConnectTimeout(Duration.ofMillis(properties.getPlatform().getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getPlatform().getReadTimeoutMs()))
                .build();
    }
}
