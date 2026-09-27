package com.evops.exchange;

import com.evops.exchange.client.PlatformClient;
import com.evops.exchange.client.PlatformSendResult;
import com.evops.exchange.entity.ExchangeMessage;
import com.evops.exchange.enums.ExchangeDirection;
import com.evops.exchange.enums.ExchangeMessageStatus;
import com.evops.exchange.enums.ExchangeMessageType;
import com.evops.exchange.mapper.ExchangeMessageMapper;
import com.evops.exchange.service.OutboundSendService;
import com.evops.exchange.xml.FuYinOrderMessage;
import com.evops.exchange.xml.FuYinOrderReply;
import com.evops.exchange.xml.JaxbConverter;
import com.evops.printorder.dto.PrintOrderCreateRequest;
import com.evops.printorder.entity.PrintOrder;
import com.evops.printorder.service.PrintOrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 出站发送失败重试：
 * 首次失败置 RETRYING 并排期退避；手工/定时重试成功后转 SENT，平台回执原文反向归档；
 * 重试到顶（max-retry=2）转 DEAD。
 */
@SpringBootTest
class OutboundRetryTest {

    @Autowired
    private OutboundSendService outboundSendService;
    @Autowired
    private PrintOrderService printOrderService;
    @Autowired
    private ExchangeMessageMapper messageMapper;

    @MockBean
    private PlatformClient platformClient;

    @BeforeEach
    void cleanArchiveDir() throws Exception {
        Path root = Paths.get("target/test-exchange-archive");
        if (Files.exists(root)) {
            Files.walk(root).sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.delete(p); } catch (Exception ignore) { }
            });
        }
    }

    @Test
    void failOnce_thenRetrySucceeds_andReplyArchived() {
        PrintOrder order = createOrder("PO-OUT-001");

        // 模拟平台：首次 500，第二次成功并回指真实出站流水号
        final String[] capturedReply = new String[1];
        Mockito.when(platformClient.sendPrintOrder(Mockito.anyString()))
                .thenReturn(PlatformSendResult.fail(500, "模拟平台 500"))
                .thenAnswer(inv -> {
                    String sentXml = inv.getArgument(0);
                    FuYinOrderMessage sent = JaxbConverter.fromXml(sentXml, FuYinOrderMessage.class);
                    capturedReply[0] = acceptedReply("RPL-OUT-1", sent.getHeader().getMessageId(), "PO-OUT-001");
                    return PlatformSendResult.ok(200, capturedReply[0]);
                });

        // 首次推送失败 -> RETRYING，记录重试次数与退避时间
        ExchangeMessage first = outboundSendService.pushOrder(order.getId());
        assertEquals(ExchangeMessageStatus.RETRYING.name(), first.getStatus());
        assertEquals(Integer.valueOf(1), first.getRetryCount());
        assertEquals(Integer.valueOf(500), first.getHttpStatus());
        assertNotNull(first.getErrorMessage());
        assertNotNull(first.getNextRetryTime(), "失败后必须排下次重试时间");
        String outMessageId = first.getMessageId();

        // 出站原文已按印单号归档落盘
        Path dir = Paths.get("target/test-exchange-archive/PO-OUT-001");
        assertTrue(Files.isDirectory(dir));

        // 手工立即重试 -> SENT；retryCount 记失败投递次数（=1），成功后不再排期
        ExchangeMessage retried = outboundSendService.retryNow(first.getId());
        assertEquals(ExchangeMessageStatus.SENT.name(), retried.getStatus());
        assertEquals(Integer.valueOf(1), retried.getRetryCount());
        assertNull(retried.getNextRetryTime());
        assertNotNull(retried.getSentTime());

        // 平台回执原文按入站反向归档
        ExchangeMessage reply = messageMapper.selectOne(new LambdaQueryWrapper<ExchangeMessage>()
                .eq(ExchangeMessage::getDirection, ExchangeDirection.IN.name())
                .eq(ExchangeMessage::getMessageType, ExchangeMessageType.PRINT_ORDER_REPLY.name())
                .eq(ExchangeMessage::getRefMessageId, outMessageId)
                .last("LIMIT 1"));
        assertNotNull(reply, "平台成功回执原文必须反向留档");
        assertEquals("PO-OUT-001", reply.getOrderNo());
        assertEquals(ExchangeMessageStatus.RECEIVED.name(), reply.getStatus());
        assertEquals(capturedReply[0], messageMapper.selectPayloadById(reply.getId()), "回执留档须为原文");
    }

    @Test
    void alwaysFailing_reachesMaxRetry_andGoesDead() {
        PrintOrder order = createOrder("PO-OUT-DEAD");
        Mockito.when(platformClient.sendPrintOrder(Mockito.anyString()))
                .thenReturn(PlatformSendResult.fail(0, "连接被拒绝"));

        ExchangeMessage first = outboundSendService.pushOrder(order.getId());
        assertEquals(ExchangeMessageStatus.RETRYING.name(), first.getStatus());
        assertEquals(Integer.valueOf(1), first.getRetryCount());

        ExchangeMessage second = outboundSendService.retryNow(first.getId());
        assertEquals(ExchangeMessageStatus.DEAD.name(), second.getStatus(), "重试到顶应转 DEAD");
        assertEquals(Integer.valueOf(2), second.getRetryCount());
        assertNull(second.getNextRetryTime(), "DEAD 不再排期");
        assertTrue(second.getErrorMessage().contains("连接被拒绝"));
    }

    private PrintOrder createOrder(String orderNo) {
        PrintOrderCreateRequest req = new PrintOrderCreateRequest();
        req.setOrderNo(orderNo);
        req.setCustomerName("客户");
        req.setPrintDate(LocalDate.of(2026, 9, 26));
        req.setPrintQty(5000L);
        req.setKai(8);
        req.setPaperLengthMm(889);
        req.setPaperWidthMm(1194);
        req.setGsm(120);
        req.setWasteRate(new BigDecimal("0.03"));
        req.setAddRate(new BigDecimal("0.01"));
        req.setAmount(new BigDecimal("600"));
        return printOrderService.create(req);
    }

    private String acceptedReply(String replyId, String refId, String orderNo) {
        FuYinOrderReply reply = new FuYinOrderReply();
        reply.setMessageId(replyId);
        reply.setRefMessageId(refId);
        reply.setOrderNo(orderNo);
        reply.setResult("ACCEPTED");
        reply.setReplyTime(LocalDateTime.of(2026, 9, 26, 9, 0, 0));
        return JaxbConverter.toXml(reply);
    }
}
