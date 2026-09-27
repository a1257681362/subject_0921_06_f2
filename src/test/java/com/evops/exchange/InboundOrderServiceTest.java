package com.evops.exchange;

import com.evops.exchange.entity.ExchangeMessage;
import com.evops.exchange.enums.ExchangeDirection;
import com.evops.exchange.mapper.ExchangeMessageMapper;
import com.evops.exchange.service.InboundOrderService;
import com.evops.exchange.xml.FuYinOrderData;
import com.evops.exchange.xml.FuYinOrderMessage;
import com.evops.exchange.xml.FuYinOrderReply;
import com.evops.exchange.xml.JaxbConverter;
import com.evops.machine.entity.Machine;
import com.evops.machine.mapper.MachineMapper;
import com.evops.printorder.dto.PrintOrderCreateRequest;
import com.evops.printorder.mapper.PrintOrderMapper;
import com.evops.printorder.service.PrintOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 入站受理：原文留档、按印单号文件归档、往返建档不丢字段、
 * messageId 幂等回放、机台号不存在/印单号重复回 REJECTED。
 */
@SpringBootTest
class InboundOrderServiceTest {

    @Autowired
    private InboundOrderService inboundOrderService;
    @Autowired
    private ExchangeMessageMapper messageMapper;
    @Autowired
    private MachineMapper machineMapper;
    @Autowired
    private PrintOrderService printOrderService;
    @Autowired
    private PrintOrderMapper printOrderMapper;

    /** 归档落盘在 target 下会跨运行累积，每个用例前清空，保证文件数断言只反映本次运行 */
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
    void receives_archivesRaw_createsOrder_andRepliesAccepted() throws Exception {
        ensureMachine("J-IN-01");
        String rawXml = sampleXml("MSG-IN-OK", "PO-IN-001", "J-IN-01");

        String replyXml = inboundOrderService.receive(rawXml);
        FuYinOrderReply reply = JaxbConverter.fromXml(replyXml, FuYinOrderReply.class);

        assertEquals("ACCEPTED", reply.getResult());
        assertEquals("MSG-IN-OK", reply.getRefMessageId());
        assertEquals("PO-IN-001", reply.getOrderNo());

        // 印单确实建档，金额等字段不丢
        com.evops.printorder.entity.PrintOrder order =
                printOrderMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.evops.printorder.entity.PrintOrder>()
                        .eq(com.evops.printorder.entity.PrintOrder::getOrderNo, "PO-IN-001"));
        assertNotNull(order);
        assertEquals(0, new BigDecimal("1234.50").compareTo(order.getAmount()));
        assertEquals(Long.valueOf(10000L), order.getPrintQty());

        // 入站原文整存，摘要齐全
        ExchangeMessage inMsg = messageMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExchangeMessage>()
                        .eq(ExchangeMessage::getMessageId, "MSG-IN-OK"));
        assertNotNull(inMsg);
        assertEquals(ExchangeDirection.IN.name(), inMsg.getDirection());
        assertEquals("RECEIVED", inMsg.getStatus());
        assertEquals("PO-IN-001", inMsg.getOrderNo());
        assertEquals(rawXml, messageMapper.selectPayloadById(inMsg.getId()), "留档必须是报文原文");
        assertNotNull(inMsg.getPayloadDigest());
        assertEquals(64, inMsg.getPayloadDigest().length());

        // 文件按印单号归档落盘（目录内同时有入站付印报文与出站回执，这里只核对入站付印报文）
        Path archived = Paths.get("target/test-exchange-archive/PO-IN-001");
        assertTrue(Files.isDirectory(archived), "应按印单号建归档目录");
        List<Path> files = Files.list(archived)
                .filter(p -> p.getFileName().toString().endsWith(".xml"))
                .filter(p -> p.getFileName().toString().contains("IN-PRINT_ORDER-MSG-IN-OK"))
                .collect(java.util.stream.Collectors.toList());
        assertEquals(1, files.size(), "该入站付印报文应归档一个 XML 文件");
        assertEquals(rawXml, new String(Files.readAllBytes(files.get(0)),
                java.nio.charset.StandardCharsets.UTF_8), "归档文件内容应为报文原文");
        assertTrue(inMsg.getArchivePath().contains("PO-IN-001/"), "登记的归档路径应按印单号组织");
    }

    @Test
    void duplicateMessageId_replaysIdempotently_withoutSecondOrder() {
        ensureMachine("J-IN-02");
        String xml = sampleXml("MSG-IN-DUP", "PO-IN-DUP", "J-IN-02");

        String first = inboundOrderService.receive(xml);
        String second = inboundOrderService.receive(xml);

        assertEquals("ACCEPTED", JaxbConverter.fromXml(first, FuYinOrderReply.class).getResult());
        FuYinOrderReply secondReply = JaxbConverter.fromXml(second, FuYinOrderReply.class);
        assertEquals("ACCEPTED", secondReply.getResult());
        assertTrue(secondReply.getReason().contains("重复"), "重复推送应幂等回放并说明");

        long count = printOrderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.evops.printorder.entity.PrintOrder>()
                        .eq(com.evops.printorder.entity.PrintOrder::getOrderNo, "PO-IN-DUP"));
        assertEquals(1L, count, "重复报文不得重复建档");
    }

    @Test
    void unknownMachineCode_isRejected_butRawKept() {
        String xml = sampleXml("MSG-IN-BADM", "PO-IN-BADM", "J-NOT-EXIST");

        String replyXml = inboundOrderService.receive(xml);
        FuYinOrderReply reply = JaxbConverter.fromXml(replyXml, FuYinOrderReply.class);
        assertEquals("REJECTED", reply.getResult());
        assertTrue(reply.getReason().contains("机台号"));

        ExchangeMessage inMsg = messageMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExchangeMessage>()
                        .eq(ExchangeMessage::getMessageId, "MSG-IN-BADM"));
        assertEquals("REJECTED", inMsg.getStatus());
        assertEquals(xml, messageMapper.selectPayloadById(inMsg.getId()), "被拒绝的报文原文也必须保留");
    }

    @Test
    void duplicateOrderNumber_isRejected() {
        ensureMachine("J-IN-03");
        // 先直接建一张同号印单
        PrintOrderCreateRequest req = new PrintOrderCreateRequest();
        req.setOrderNo("PO-IN-EXIST");
        req.setPrintQty(100L);
        req.setKai(16);
        req.setPaperLengthMm(889);
        req.setPaperWidthMm(1194);
        req.setGsm(100);
        printOrderService.create(req);

        String xml = sampleXml("MSG-IN-EXIST", "PO-IN-EXIST", "J-IN-03");
        FuYinOrderReply reply = JaxbConverter.fromXml(inboundOrderService.receive(xml), FuYinOrderReply.class);
        assertEquals("REJECTED", reply.getResult());
    }

    private void ensureMachine(String code) {
        Long count = machineMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Machine>()
                        .eq(Machine::getMachineCode, code));
        if (count != null && count == 0L) {
            Machine m = new Machine();
            m.setMachineCode(code);
            m.setMachineName("测试机台" + code);
            m.setStatus("ACTIVE");
            machineMapper.insert(m);
        }
    }

    private String sampleXml(String messageId, String orderNo, String machineCode) {
        FuYinOrderMessage msg = new FuYinOrderMessage();
        msg.setHeader(new FuYinOrderMessage.Header(
                messageId, "SELF-PORTAL", LocalDateTime.of(2026, 9, 26, 8, 0, 0), "PRINT_ORDER"));
        FuYinOrderData data = new FuYinOrderData();
        data.setOrderNo(orderNo);
        data.setCustomerName("客户" + orderNo);
        data.setProductName("印品");
        data.setMachineCode(machineCode);
        data.setPrintDate(LocalDate.of(2026, 9, 26));
        data.setPrintQty(10000L);
        data.setKai(16);
        data.setPaperLengthMm(889);
        data.setPaperWidthMm(1194);
        data.setGsm(157);
        data.setWasteRate(new BigDecimal("0.05"));
        data.setAddRate(new BigDecimal("0.02"));
        data.setAmount(new BigDecimal("1234.5"));
        msg.setBody(data);
        return JaxbConverter.toXml(msg);
    }
}
