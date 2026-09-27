package com.evops.exchange.service;

import com.evops.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * HTTP 发送通道：POST application/xml 到 exchange.gateway-url。
 * 地址未配置、连接/读超时、对方返回非 2xx，一律视为发送失败（可重试）。
 */
@Component
public class HttpExchangeGatewayClient implements ExchangeGatewayClient {

    private static final MediaType APPLICATION_XML_UTF8 =
            new MediaType(MediaType.APPLICATION_XML, StandardCharsets.UTF_8);

    private final String gatewayUrl;
    private final RestTemplate restTemplate;

    public HttpExchangeGatewayClient(@Value("${exchange.gateway-url:}") String gatewayUrl,
                                     RestTemplateBuilder builder) {
        this.gatewayUrl = gatewayUrl;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public void send(String xml) {
        if (!StringUtils.hasText(gatewayUrl)) {
            throw new BusinessException("未配置客户自助下单平台地址 exchange.gateway-url，报文已归档待重试");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_XML_UTF8);
        ResponseEntity<String> response =
                restTemplate.postForEntity(gatewayUrl, new HttpEntity<>(xml, headers), String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new BusinessException("客户平台返回异常状态码：" + response.getStatusCodeValue());
        }
    }
}
