package com.evops.exchange.controller;

import com.evops.common.ApiResponse;
import com.evops.common.PageResult;
import com.evops.exchange.dto.ExchangeMessageQuery;
import com.evops.exchange.entity.ExchangeMessage;
import com.evops.exchange.message.PrintOrderMessage;
import com.evops.exchange.service.ExchangeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exchange")
public class ExchangeController {

    private final ExchangeService exchangeService;

    public ExchangeController(ExchangeService exchangeService) {
        this.exchangeService = exchangeService;
    }

    /** 生成付印报文并发送：先归档留原文，再调客户平台，按结果更新状态 */
    @PostMapping("/print-orders/{id}/send")
    public ApiResponse<ExchangeMessage> send(@PathVariable Long id) {
        return ApiResponse.ok(exchangeService.sendPrintOrder(id));
    }

    /** 失败重试：重发归档原文，仅待发送/发送失败可重试 */
    @PostMapping("/messages/{id}/retry")
    public ApiResponse<ExchangeMessage> retry(@PathVariable Long id) {
        return ApiResponse.ok(exchangeService.retry(id));
    }

    /** 报文归档检索：按印单号（+方向/状态）组合分页 */
    @GetMapping("/messages")
    public ApiResponse<PageResult<ExchangeMessage>> page(@ModelAttribute ExchangeMessageQuery query) {
        return ApiResponse.ok(exchangeService.page(query));
    }

    /** 报文详情（含原文） */
    @GetMapping("/messages/{id}")
    public ApiResponse<ExchangeMessage> detail(@PathVariable Long id) {
        return ApiResponse.ok(exchangeService.getById(id));
    }

    /** 接收客户自助下单平台来文：解析校验 → 按印单号归档留原文 */
    @PostMapping(value = "/inbound", consumes = MediaType.APPLICATION_XML_VALUE)
    public ApiResponse<PrintOrderMessage> inbound(@RequestBody String xml) {
        return ApiResponse.ok(exchangeService.receive(xml));
    }
}
