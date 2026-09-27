package com.evops.exchange.message;

import com.evops.exchange.message.adapter.AmountXmlAdapter;
import com.evops.exchange.message.adapter.LocalDateTimeXmlAdapter;
import com.evops.exchange.message.adapter.LocalDateXmlAdapter;
import com.evops.exchange.message.adapter.RateXmlAdapter;
import com.evops.exchange.message.adapter.WeightXmlAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 付印数据报文（与客户自助下单平台交换）。
 * 格式规范：日期 yyyy-MM-dd，时间 yyyy-MM-dd HH:mm:ss，
 * 金额定点 2 位小数，比率 4 位，重量 3 位。
 * propOrder 固定元素顺序，保证同一内容序列化文本恒定（往返转换不丢内容）。
 */
@XmlRootElement(name = "printOrderMessage")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
        "header", "orderNo", "customerName", "productName", "machineCode", "printDate",
        "printQty", "kai", "paperLengthMm", "paperWidthMm", "gsm",
        "wasteRate", "addRate",
        "baseSheets", "wasteSheets", "addSheets", "totalSheets", "paperWeightKg",
        "orderAmount", "currency", "status", "remark"
})
public class PrintOrderMessage {

    @XmlElement(required = true)
    private Header header;

    private String orderNo;
    private String customerName;
    private String productName;
    private String machineCode;

    @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
    private LocalDate printDate;

    private Long printQty;
    private Integer kai;
    private Integer paperLengthMm;
    private Integer paperWidthMm;
    private Integer gsm;

    @XmlJavaTypeAdapter(RateXmlAdapter.class)
    private BigDecimal wasteRate;

    @XmlJavaTypeAdapter(RateXmlAdapter.class)
    private BigDecimal addRate;

    private Long baseSheets;
    private Long wasteSheets;
    private Long addSheets;
    private Long totalSheets;

    @XmlJavaTypeAdapter(WeightXmlAdapter.class)
    private BigDecimal paperWeightKg;

    /** 订单金额（平台来文可携带），定点 2 位小数 */
    @XmlJavaTypeAdapter(AmountXmlAdapter.class)
    private BigDecimal orderAmount;

    private String currency;
    private String status;
    private String remark;

    /** 报文头：报文ID（唯一，重复推送拒绝）、报文类型、发出时间 */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Header {
        @XmlElement(required = true)
        private String messageId;
        private String messageType;

        @XmlJavaTypeAdapter(LocalDateTimeXmlAdapter.class)
        private LocalDateTime sentTime;

        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
        public LocalDateTime getSentTime() { return sentTime; }
        public void setSentTime(LocalDateTime sentTime) { this.sentTime = sentTime; }
    }

    public Header getHeader() { return header; }
    public void setHeader(Header header) { this.header = header; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getMachineCode() { return machineCode; }
    public void setMachineCode(String machineCode) { this.machineCode = machineCode; }
    public LocalDate getPrintDate() { return printDate; }
    public void setPrintDate(LocalDate printDate) { this.printDate = printDate; }
    public Long getPrintQty() { return printQty; }
    public void setPrintQty(Long printQty) { this.printQty = printQty; }
    public Integer getKai() { return kai; }
    public void setKai(Integer kai) { this.kai = kai; }
    public Integer getPaperLengthMm() { return paperLengthMm; }
    public void setPaperLengthMm(Integer paperLengthMm) { this.paperLengthMm = paperLengthMm; }
    public Integer getPaperWidthMm() { return paperWidthMm; }
    public void setPaperWidthMm(Integer paperWidthMm) { this.paperWidthMm = paperWidthMm; }
    public Integer getGsm() { return gsm; }
    public void setGsm(Integer gsm) { this.gsm = gsm; }
    public BigDecimal getWasteRate() { return wasteRate; }
    public void setWasteRate(BigDecimal wasteRate) { this.wasteRate = wasteRate; }
    public BigDecimal getAddRate() { return addRate; }
    public void setAddRate(BigDecimal addRate) { this.addRate = addRate; }
    public Long getBaseSheets() { return baseSheets; }
    public void setBaseSheets(Long baseSheets) { this.baseSheets = baseSheets; }
    public Long getWasteSheets() { return wasteSheets; }
    public void setWasteSheets(Long wasteSheets) { this.wasteSheets = wasteSheets; }
    public Long getAddSheets() { return addSheets; }
    public void setAddSheets(Long addSheets) { this.addSheets = addSheets; }
    public Long getTotalSheets() { return totalSheets; }
    public void setTotalSheets(Long totalSheets) { this.totalSheets = totalSheets; }
    public BigDecimal getPaperWeightKg() { return paperWeightKg; }
    public void setPaperWeightKg(BigDecimal paperWeightKg) { this.paperWeightKg = paperWeightKg; }
    public BigDecimal getOrderAmount() { return orderAmount; }
    public void setOrderAmount(BigDecimal orderAmount) { this.orderAmount = orderAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
