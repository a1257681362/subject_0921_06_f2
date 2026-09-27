package com.evops.exchange.client;

import com.evops.exchange.config.ExchangeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 客户自助下单平台 HTTP 客户端：以 application/xml;charset=UTF-8 投递报文原文。
 * 不抛异常给上层，网络异常/非 2xx 统一转 PlatformSendResult.fail，交给重试机制处理。
 */
@Component
public class PlatformClient {

    private static final Logger log = LoggerFactory.getLogger(PlatformClient.class);
    private static final MediaType XML_UTF8 = MediaType.parseMediaType("application/xml;charset=UTF-8");

    private final RestTemplate restTemplate;
    private final ExchangeProperties properties;

    public PlatformClient(@Qualifier("exchangeRestTemplate") RestTemplate restTemplate,
                          ExchangeProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public PlatformSendResult sendPrintOrder(String payload) {
        return post(properties.getPlatform().getEndpoint(), payload);
    }

    private PlatformSendResult post(String url, String payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(XML_UTF8);
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_XML));

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);

            if (resp.getStatusCode().is2xxSuccessful()) {
                return PlatformSendResult.ok(resp.getStatusCodeValue(), resp.getBody());
            }
            return PlatformSendResult.fail(resp.getStatusCodeValue(),
                    "平台返回非成功状态码 " + resp.getStatusCodeValue());
        } catch (HttpStatusCodeException e) {
            // 4xx/5xx：有响应体带出，便于排障
            String body = e.getResponseBodyAsString();
            log.warn("平台返回错误状态 {}: {}", e.getRawStatusCode(), body);
            return PlatformSendResult.fail(e.getRawStatusCode(),
                    "平台返回状态码 " + e.getRawStatusCode()
                            + (body == null || body.isEmpty() ? "" : ": " + truncate(body, 800)));
        } catch (ResourceAccessException e) {
            // 连不上、读超时：典型可重试故障
            log.warn("平台连接/超时失败: {}", e.getMessage());
            return PlatformSendResult.fail(0, "平台连接或超时失败: " + truncate(String.valueOf(e.getMessage()), 800));
        } catch (RestClientException e) {
            log.warn("平台调用失败: {}", e.getMessage());
            return PlatformSendResult.fail(0, "平台调用异常: " + truncate(String.valueOf(e.getMessage()), 800));
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
