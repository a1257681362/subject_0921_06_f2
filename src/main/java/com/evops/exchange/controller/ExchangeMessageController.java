package com.evops.exchange.controller;

import com.evops.common.ApiResponse;
import com.evops.common.PageResult;
import com.evops.exchange.dto.ExchangeMessageQuery;
import com.evops.exchange.entity.ExchangeMessage;
import com.evops.exchange.service.ExchangeMessageQueryService;
import com.evops.exchange.service.OutboundSendService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 报文交换管理：按印单号检索归档、调阅原文、手工推送印单、手工重试失败报文。
 */
@RestController
@RequestMapping("/api/exchange")
public class ExchangeMessageController {

    private final ExchangeMessageQueryService queryService;
    private final OutboundSendService outboundSendService;

    public ExchangeMessageController(ExchangeMessageQueryService queryService,
                                     OutboundSendService outboundSendService) {
        this.queryService = queryService;
        this.outboundSendService = outboundSendService;
    }

    /** 按印单号 + 方向/状态/类型组合分页检索报文归档 */
    @GetMapping("/messages")
    public ApiResponse<PageResult<ExchangeMessage>> page(@ModelAttribute ExchangeMessageQuery query) {
        return ApiResponse.ok(queryService.page(query));
    }

    /** 报文元数据详情（不含原文） */
    @GetMapping("/messages/{id}")
    public ApiResponse<ExchangeMessage> detail(@PathVariable Long id) {
        return ApiResponse.ok(queryService.getById(id));
    }

    /** 下载报文原文（留档原文整取，Content-Type application/xml） */
    @GetMapping(value = "/messages/{id}/payload", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> payload(@PathVariable Long id) {
        ExchangeMessage meta = queryService.getById(id);
        String payload = queryService.getPayload(id);
        String filename = meta.getOrderNo() + "-" + meta.getDirection()
                + "-" + meta.getMessageId() + ".xml";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/xml;charset=UTF-8"))
                .body(payload.getBytes(StandardCharsets.UTF_8));
    }

    /** 把一张印单组装成付印报文推送客户平台（失败自动进入退避重试） */
    @PostMapping("/outbound/orders/{orderId}/push")
    public ApiResponse<ExchangeMessage> push(@PathVariable Long orderId) {
        return ApiResponse.ok(outboundSendService.pushOrder(orderId));
    }

    /** 手工立即重试失败（RETRYING/DEAD）报文 */
    @PostMapping("/messages/{id}/retry")
    public ApiResponse<ExchangeMessage> retry(@PathVariable Long id) {
        return ApiResponse.ok(outboundSendService.retryNow(id));
    }
}
