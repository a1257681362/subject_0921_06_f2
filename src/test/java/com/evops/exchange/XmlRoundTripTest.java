package com.evops.exchange;

import com.evops.exchange.xml.FuYinOrderData;
import com.evops.exchange.xml.FuYinOrderMessage;
import com.evops.exchange.xml.JaxbConverter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * XML 序列化规范 + 往返转换不丢失：
 * 日期 yyyy-MM-dd、时间 yyyy-MM-dd HH:mm:ss、金额两位小数、费率四位小数；
 * 序列化后再反序列化，业务字段逐项一致。
 */
class XmlRoundTripTest {

    @Test
    void formatsAreSpecCompliant_andRoundTripLosesNothing() {
        FuYinOrderMessage message = new FuYinOrderMessage();
        message.setHeader(new FuYinOrderMessage.Header(
                "M-001", "SELF-PORTAL",
                LocalDateTime.of(2026, 9, 26, 10, 20, 30),
                "PRINT_ORDER"));
        FuYinOrderData data = new FuYinOrderData();
        data.setOrderNo("PO-RT-001");
        data.setCustomerName("往返客户");
        data.setProductName("宣传册");
        data.setMachineCode("J-01");
        data.setPrintDate(LocalDate.of(2026, 9, 26));
        data.setPrintQty(10000L);
        data.setKai(16);
        data.setPaperLengthMm(889);
        data.setPaperWidthMm(1194);
        data.setGsm(157);
        data.setWasteRate(new BigDecimal("0.05"));
        data.setAddRate(new BigDecimal("0.02"));
        data.setAmount(new BigDecimal("1234.5"));
        data.setRemark("往返备注");
        message.setBody(data);

        String xml = JaxbConverter.toXml(message);

        // 格式规范断言
        assertTrue(xml.contains("<?xml"), "应带 XML 声明");
        assertTrue(xml.contains("<printDate>2026-09-26</printDate>"), "日期应为 yyyy-MM-dd");
        assertTrue(xml.contains("<sendTime>2026-09-26 10:20:30</sendTime>"), "时间应为 yyyy-MM-dd HH:mm:ss");
        assertTrue(xml.contains("<wasteRate>0.0500</wasteRate>"), "费率应固定四位小数");
        assertTrue(xml.contains("<addRate>0.0200</addRate>"), "费率应固定四位小数");
        assertTrue(xml.contains("<amount>1234.50</amount>"), "金额应固定两位小数");

        // 往返不丢字段
        FuYinOrderMessage back = JaxbConverter.fromXml(xml, FuYinOrderMessage.class);
        assertEquals("M-001", back.getHeader().getMessageId());
        assertEquals("SELF-PORTAL", back.getHeader().getSender());
        assertEquals(LocalDateTime.of(2026, 9, 26, 10, 20, 30), back.getHeader().getSendTime());

        FuYinOrderData b = back.getBody();
        assertEquals("PO-RT-001", b.getOrderNo());
        assertEquals("往返客户", b.getCustomerName());
        assertEquals("宣传册", b.getProductName());
        assertEquals("J-01", b.getMachineCode());
        assertEquals(LocalDate.of(2026, 9, 26), b.getPrintDate());
        assertEquals(Long.valueOf(10000L), b.getPrintQty());
        assertEquals(Integer.valueOf(16), b.getKai());
        assertEquals(Integer.valueOf(889), b.getPaperLengthMm());
        assertEquals(Integer.valueOf(1194), b.getPaperWidthMm());
        assertEquals(Integer.valueOf(157), b.getGsm());
        assertEquals(0, new BigDecimal("0.0500").compareTo(b.getWasteRate()), "费率精度往返一致");
        assertEquals(0, new BigDecimal("0.0200").compareTo(b.getAddRate()), "费率精度往返一致");
        assertEquals(0, new BigDecimal("1234.50").compareTo(b.getAmount()), "金额精度往返一致");
        assertEquals("往返备注", b.getRemark());
        assertNotNull(b);
    }

    @Test
    void parsesLooseAmountScalesWithoutLoss() {
        // 对方金额给了非两位标度，接收方应保留值（规整为两位），不丢数值
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<FuYinMessage><header><messageId>M1</messageId></header>"
                + "<body><orderNo>PO1</orderNo><amount>88</amount>"
                + "<wasteRate>0.1</wasteRate></body></FuYinMessage>";
        FuYinOrderMessage m = JaxbConverter.fromXml(xml, FuYinOrderMessage.class);
        assertEquals(0, new BigDecimal("88.00").compareTo(m.getBody().getAmount()));
        assertEquals(0, new BigDecimal("0.1000").compareTo(m.getBody().getWasteRate()));
    }
}
