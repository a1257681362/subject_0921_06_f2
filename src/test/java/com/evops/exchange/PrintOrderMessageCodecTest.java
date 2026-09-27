package com.evops.exchange;

import com.evops.common.BusinessException;
import com.evops.exchange.message.PrintOrderMessage;
import com.evops.exchange.message.PrintOrderMessageCodec;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 报文编解码：往返转换内容不丢失（对象→XML→对象→XML 文本恒定），
 * 日期/时间/金额/比率/重量按规范格式输出。
 */
class PrintOrderMessageCodecTest {

    private final PrintOrderMessageCodec codec = new PrintOrderMessageCodec();

    @Test
    void roundTrip_keepsContent_andProducesIdenticalXml() {
        PrintOrderMessage message = sampleMessage();

        String xml = codec.toXml(message);
        PrintOrderMessage parsed = codec.parse(xml);

        // 逐字段核对：往返转换内容不丢失
        assertEquals(message.getHeader().getMessageId(), parsed.getHeader().getMessageId());
        assertEquals(message.getHeader().getMessageType(), parsed.getHeader().getMessageType());
        assertEquals(message.getHeader().getSentTime(), parsed.getHeader().getSentTime());
        assertEquals(message.getOrderNo(), parsed.getOrderNo());
        assertEquals(message.getCustomerName(), parsed.getCustomerName());
        assertEquals(message.getProductName(), parsed.getProductName());
        assertEquals(message.getMachineCode(), parsed.getMachineCode());
        assertEquals(message.getPrintDate(), parsed.getPrintDate());
        assertEquals(message.getPrintQty(), parsed.getPrintQty());
        assertEquals(message.getKai(), parsed.getKai());
        assertEquals(message.getPaperLengthMm(), parsed.getPaperLengthMm());
        assertEquals(message.getPaperWidthMm(), parsed.getPaperWidthMm());
        assertEquals(message.getGsm(), parsed.getGsm());
        assertEquals(0, message.getWasteRate().compareTo(parsed.getWasteRate()));
        assertEquals(0, message.getAddRate().compareTo(parsed.getAddRate()));
        assertEquals(message.getBaseSheets(), parsed.getBaseSheets());
        assertEquals(message.getWasteSheets(), parsed.getWasteSheets());
        assertEquals(message.getAddSheets(), parsed.getAddSheets());
        assertEquals(message.getTotalSheets(), parsed.getTotalSheets());
        assertEquals(0, message.getPaperWeightKg().compareTo(parsed.getPaperWeightKg()));
        assertEquals(0, message.getOrderAmount().compareTo(parsed.getOrderAmount()));
        assertEquals(message.getCurrency(), parsed.getCurrency());
        assertEquals(message.getStatus(), parsed.getStatus());
        assertEquals(message.getRemark(), parsed.getRemark());

        // 再序列化：XML 文本与第一次完全一致
        assertEquals(xml, codec.toXml(parsed));
    }

    @Test
    void formats_datesAndAmountsPerSpec() {
        String xml = codec.toXml(sampleMessage());

        assertTrue(xml.contains("<sentTime>2026-09-26 10:30:45</sentTime>"), "时间须为 yyyy-MM-dd HH:mm:ss");
        assertTrue(xml.contains("<printDate>2026-09-26</printDate>"), "日期须为 yyyy-MM-dd");
        assertTrue(xml.contains("<wasteRate>0.0500</wasteRate>"), "比率须定点 4 位小数");
        assertTrue(xml.contains("<addRate>0.0200</addRate>"), "比率须定点 4 位小数");
        assertTrue(xml.contains("<paperWeightKg>111.590</paperWeightKg>"), "重量须定点 3 位小数");
        assertTrue(xml.contains("<orderAmount>12345.60</orderAmount>"), "金额须定点 2 位小数");
    }

    @Test
    void parse_readsExternalPlatformXml_andNormalizesAmounts() {
        // 模拟客户自助下单平台来文：金额 1 位小数、比率 2 位小数，解析后按规范归一
        String inbound = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<printOrderMessage>\n"
                + "    <header>\n"
                + "        <messageId>MSG-PLATFORM-0001</messageId>\n"
                + "        <messageType>PRINT_ORDER</messageType>\n"
                + "        <sentTime>2026-09-26 09:00:00</sentTime>\n"
                + "    </header>\n"
                + "    <orderNo>PO-EXT-0001</orderNo>\n"
                + "    <customerName>平台客户</customerName>\n"
                + "    <printDate>2026-10-01</printDate>\n"
                + "    <printQty>5000</printQty>\n"
                + "    <kai>8</kai>\n"
                + "    <orderAmount>888.8</orderAmount>\n"
                + "    <currency>CNY</currency>\n"
                + "    <wasteRate>0.05</wasteRate>\n"
                + "</printOrderMessage>";

        PrintOrderMessage parsed = codec.parse(inbound);
        assertEquals("MSG-PLATFORM-0001", parsed.getHeader().getMessageId());
        assertEquals("PO-EXT-0001", parsed.getOrderNo());
        assertEquals(LocalDate.of(2026, 10, 1), parsed.getPrintDate());
        assertEquals(0, new BigDecimal("888.8").compareTo(parsed.getOrderAmount()));

        // 归一后按规范输出，往返文本恒定
        String out = codec.toXml(parsed);
        assertTrue(out.contains("<orderAmount>888.80</orderAmount>"));
        assertTrue(out.contains("<wasteRate>0.0500</wasteRate>"));
        assertEquals(out, codec.toXml(codec.parse(out)));
    }

    @Test
    void parse_rejectsDoctype_andBlankContent() {
        String xxe = "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE printOrderMessage [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>\n"
                + "<printOrderMessage><orderNo>&x;</orderNo></printOrderMessage>";
        assertThrows(BusinessException.class, () -> codec.parse(xxe), "DOCTYPE 必须拒绝（防 XXE）");

        assertThrows(BusinessException.class, () -> codec.parse("   "));
        assertThrows(BusinessException.class, () -> codec.parse("<not-closed"));
    }

    @Test
    void nullFields_areOmitted_andRoundTripStable() {
        PrintOrderMessage message = new PrintOrderMessage();
        PrintOrderMessage.Header header = new PrintOrderMessage.Header();
        header.setMessageId("MSG-MIN-0001");
        header.setMessageType("PRINT_ORDER");
        header.setSentTime(LocalDateTime.of(2026, 9, 26, 8, 0, 0));
        message.setHeader(header);
        message.setOrderNo("PO-MIN-0001");

        String xml = codec.toXml(message);
        PrintOrderMessage parsed = codec.parse(xml);
        assertNull(parsed.getPrintDate());
        assertNull(parsed.getOrderAmount());
        assertNull(parsed.getRemark());
        assertEquals(xml, codec.toXml(parsed));
    }

    private PrintOrderMessage sampleMessage() {
        PrintOrderMessage message = new PrintOrderMessage();
        PrintOrderMessage.Header header = new PrintOrderMessage.Header();
        header.setMessageId("MSG-TEST-0001");
        header.setMessageType("PRINT_ORDER");
        header.setSentTime(LocalDateTime.of(2026, 9, 26, 10, 30, 45));
        message.setHeader(header);
        message.setOrderNo("PO-2026-0001");
        message.setCustomerName("示例客户 & 合伙人");
        message.setProductName("宣传册 <A4>");
        message.setMachineCode("JM-01");
        message.setPrintDate(LocalDate.of(2026, 9, 26));
        message.setPrintQty(10000L);
        message.setKai(16);
        message.setPaperLengthMm(889);
        message.setPaperWidthMm(1194);
        message.setGsm(157);
        message.setWasteRate(new BigDecimal("0.05"));
        message.setAddRate(new BigDecimal("0.02"));
        message.setBaseSheets(625L);
        message.setWasteSheets(32L);
        message.setAddSheets(13L);
        message.setTotalSheets(670L);
        message.setPaperWeightKg(new BigDecimal("111.59"));
        message.setOrderAmount(new BigDecimal("12345.6"));
        message.setCurrency("CNY");
        message.setStatus("ACTIVE");
        message.setRemark("含特殊字符 &<>\"' 需正确转义");
        return message;
    }
}
