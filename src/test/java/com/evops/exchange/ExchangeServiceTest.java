package com.evops.exchange;

import com.evops.common.BusinessException;
import com.evops.common.PageResult;
import com.evops.exchange.dto.ExchangeMessageQuery;
import com.evops.exchange.entity.ExchangeMessage;
import com.evops.exchange.mapper.ExchangeMessageMapper;
import com.evops.exchange.message.PrintOrderMessage;
import com.evops.exchange.message.PrintOrderMessageCodec;
import com.evops.exchange.service.ExchangeGatewayClient;
import com.evops.exchange.service.ExchangeService;
import com.evops.printorder.dto.PrintOrderCreateRequest;
import com.evops.printorder.entity.PrintOrder;
import com.evops.printorder.service.PrintOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;

/**
 * 付印数据交换：发送/失败/重试/接收/归档全链路。
 * 网关用 Mock 模拟客户自助下单平台，失败即抛异常。
 */
@SpringBootTest
class ExchangeServiceTest {

    @Autowired
    private ExchangeService exchangeService;

    @Autowired
    private PrintOrderService printOrderService;

    @Autowired
    private ExchangeMessageMapper exchangeMessageMapper;

    @Autowired
    private PrintOrderMessageCodec codec;

    @MockBean
    private ExchangeGatewayClient gatewayClient;

    @Test
    void send_success_marksSent_andArchivesRawXml() {
        PrintOrder order = printOrderService.create(sampleRequest("PO-EX-001"));

        ExchangeMessage sent = exchangeService.sendPrintOrder(order.getId());

        assertEquals("SENT", sent.getStatus());
        assertNotNull(sent.getSentTime());
        assertEquals(0, sent.getRetryCount());
        assertTrue(sent.getXmlContent().contains("<orderNo>PO-EX-001</orderNo>"));
        assertTrue(sent.getXmlContent().contains("<printDate>2026-09-26</printDate>"));

        // 归档留原文：库里的原文与发送报文逐字一致
        ExchangeMessage archived = exchangeMessageMapper.selectById(sent.getId());
        assertEquals(sent.getXmlContent(), archived.getXmlContent());
        assertEquals("OUTBOUND", archived.getDirection());
        assertEquals("PRINT_ORDER", archived.getMessageType());
    }

    @Test
    void send_failure_marksFailed_andRetryResendsArchivedOriginalXml() {
        PrintOrder order = printOrderService.create(sampleRequest("PO-EX-002"));
        BDDMockito.willThrow(new RuntimeException("连接客户平台超时"))
                .given(gatewayClient).send(anyString());

        ExchangeMessage failed = exchangeService.sendPrintOrder(order.getId());
        assertEquals("FAILED", failed.getStatus());
        assertEquals("连接客户平台超时", failed.getLastError());
        String originalXml = failed.getXmlContent();

        // 平台恢复后重试：重发归档原文，状态翻转，重试次数累加
        BDDMockito.willDoNothing().given(gatewayClient).send(anyString());
        ExchangeMessage retried = exchangeService.retry(failed.getId());

        assertEquals("SENT", retried.getStatus());
        assertEquals(1, retried.getRetryCount());
        assertEquals(originalXml, retried.getXmlContent());
        assertNotNull(retried.getSentTime());
        // 网关两次收到的都是同一份归档原文
        BDDMockito.then(gatewayClient).should(times(2)).send(originalXml);
    }

    @Test
    void retry_rejectsSentMessage_andInboundMessage() {
        PrintOrder order = printOrderService.create(sampleRequest("PO-EX-003"));
        ExchangeMessage sent = exchangeService.sendPrintOrder(order.getId());
        assertThrows(BusinessException.class, () -> exchangeService.retry(sent.getId()));

        String inboundXml = buildInboundXml("MSG-EX-003", "PO-EX-003");
        exchangeService.receive(inboundXml);
        ExchangeMessageQuery query = new ExchangeMessageQuery();
        query.setOrderNo("PO-EX-003");
        query.setDirection("INBOUND");
        ExchangeMessage inbound = exchangeService.page(query).getRecords().get(0);
        assertThrows(BusinessException.class, () -> exchangeService.retry(inbound.getId()));
    }

    @Test
    void receive_archivesRawXmlByOrderNo() {
        String xml = buildInboundXml("MSG-EX-004", "PO-EX-004");

        PrintOrderMessage parsed = exchangeService.receive(xml);
        assertEquals("PO-EX-004", parsed.getOrderNo());
        assertEquals(0, new BigDecimal("666.66").compareTo(parsed.getOrderAmount()));

        // 按印单号归档检索，原文逐字一致
        ExchangeMessageQuery query = new ExchangeMessageQuery();
        query.setOrderNo("PO-EX-004");
        PageResult<ExchangeMessage> page = exchangeService.page(query);
        assertEquals(1, page.getTotal());
        ExchangeMessage archived = page.getRecords().get(0);
        assertEquals("RECEIVED", archived.getStatus());
        assertEquals("INBOUND", archived.getDirection());
        assertEquals(xml, archived.getXmlContent());
    }

    @Test
    void receive_badXml_stillArchivedForTroubleshooting() {
        String badXml = "<printOrderMessage><broken>";
        assertThrows(BusinessException.class, () -> exchangeService.receive(badXml));

        // 解析失败的来文也留原文归档（UNKNOWN），便于与对方平台对账排查
        ExchangeMessageQuery query = new ExchangeMessageQuery();
        query.setOrderNo("UNKNOWN");
        PageResult<ExchangeMessage> page = exchangeService.page(query);
        assertEquals(1, page.getTotal());
        ExchangeMessage archived = page.getRecords().get(0);
        assertEquals("FAILED", archived.getStatus());
        assertEquals(badXml, archived.getXmlContent());
        assertNotNull(archived.getLastError());
    }

    @Test
    void receive_duplicateMessageId_rejected() {
        String xml = buildInboundXml("MSG-EX-005", "PO-EX-005");
        exchangeService.receive(xml);
        assertThrows(BusinessException.class, () -> exchangeService.receive(xml), "同一报文ID重复推送必须拒绝");
    }

    @Test
    void send_voidOrder_rejected() {
        PrintOrder order = printOrderService.create(sampleRequest("PO-EX-006"));
        printOrderService.voidOrder(order.getId(), "测试作废");
        assertThrows(BusinessException.class, () -> exchangeService.sendPrintOrder(order.getId()));
    }

    private String buildInboundXml(String messageId, String orderNo) {
        PrintOrderMessage message = new PrintOrderMessage();
        PrintOrderMessage.Header header = new PrintOrderMessage.Header();
        header.setMessageId(messageId);
        header.setMessageType("PRINT_ORDER");
        header.setSentTime(LocalDateTime.of(2026, 9, 26, 9, 0, 0));
        message.setHeader(header);
        message.setOrderNo(orderNo);
        message.setCustomerName("平台客户");
        message.setPrintDate(LocalDate.of(2026, 10, 1));
        message.setPrintQty(5000L);
        message.setKai(8);
        message.setOrderAmount(new BigDecimal("666.66"));
        message.setCurrency("CNY");
        return codec.toXml(message);
    }

    private PrintOrderCreateRequest sampleRequest(String orderNo) {
        PrintOrderCreateRequest req = new PrintOrderCreateRequest();
        req.setOrderNo(orderNo);
        req.setCustomerName("测试客户");
        req.setPrintDate(LocalDate.of(2026, 9, 26));
        req.setPrintQty(10000L);
        req.setKai(16);
        req.setPaperLengthMm(889);
        req.setPaperWidthMm(1194);
        req.setGsm(157);
        req.setWasteRate(new BigDecimal("0.05"));
        req.setAddRate(new BigDecimal("0.02"));
        return req;
    }
}
