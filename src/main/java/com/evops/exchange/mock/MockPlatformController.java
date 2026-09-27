package com.evops.exchange.mock;

import com.evops.exchange.xml.FuYinOrderMessage;
import com.evops.exchange.xml.FuYinOrderReply;
import com.evops.exchange.xml.JaxbConverter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 客户自助下单平台 mock（仅用于下周对接演示与本地联调，不属于正式交换链路）。
 * 收付印报文后回 ACCEPTED 回执；
 * 端点带 ?mockFailTimes=N 时，同一 messageId 的前 N 次投递返回 500，
 * 第 N+1 次起成功，用于演示失败退避重试到最终送达。
 * 正式对接时 exchange.platform.endpoint 换成客户真实地址即可，本端点可不暴露。
 */
@RestController
@RequestMapping("/mock/platform")
public class MockPlatformController {

    private final ConcurrentMap<String, AtomicInteger> remainingFailures = new ConcurrentHashMap<>();

    @PostMapping(value = "/orders",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE, MediaType.ALL_VALUE},
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> receive(@RequestBody String rawXml,
                                          @RequestParam(value = "mockFailTimes", required = false) Integer mockFailTimes) {
        FuYinOrderMessage message = JaxbConverter.fromXml(rawXml, FuYinOrderMessage.class);
        String messageId = message.getHeader() == null ? null : message.getHeader().getMessageId();
        String orderNo = message.getBody() == null ? null : message.getBody().getOrderNo();

        if (mockFailTimes != null && mockFailTimes > 0) {
            AtomicInteger counter = remainingFailures.computeIfAbsent(messageId,
                    k -> new AtomicInteger(mockFailTimes));
            if (counter.getAndDecrement() > 0) {
                return ResponseEntity.status(500)
                        .contentType(MediaType.parseMediaType("application/xml;charset=UTF-8"))
                        .body("<error>mock 平台模拟投递失败，等待重试</error>");
            }
        }

        FuYinOrderReply reply = new FuYinOrderReply();
        reply.setMessageId("RPL-" + UUID.randomUUID().toString().replace("-", ""));
        reply.setRefMessageId(messageId);
        reply.setOrderNo(orderNo);
        reply.setResult("ACCEPTED");
        reply.setReason("mock 平台已受理");
        reply.setReplyTime(LocalDateTime.now());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/xml;charset=UTF-8"))
                .body(JaxbConverter.toXml(reply));
    }
}
