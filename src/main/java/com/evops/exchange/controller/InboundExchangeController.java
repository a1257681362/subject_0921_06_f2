package com.evops.exchange.controller;

import com.evops.exchange.service.InboundOrderService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 客户自助下单平台 -> 本厂：付印数据入站受理。
 * 收报文原文，留档、建档后回 ACCEPTED/REJECTED 回执，全程 XML。
 */
@RestController
@RequestMapping("/api/exchange/inbound")
public class InboundExchangeController {

    private final InboundOrderService inboundOrderService;

    public InboundExchangeController(InboundOrderService inboundOrderService) {
        this.inboundOrderService = inboundOrderService;
    }

    @PostMapping(value = "/orders",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE, MediaType.ALL_VALUE},
            produces = MediaType.APPLICATION_XML_VALUE)
    public String receiveOrder(@RequestBody byte[] rawBytes) {
        // 按原始字节 UTF-8 解码留原文，不依赖对端 Content-Type 是否带 charset，避免中文被按 ISO-8859-1 误解码
        String rawXml = new String(rawBytes, StandardCharsets.UTF_8);
        return inboundOrderService.receive(rawXml);
    }
}
