package com.evops.exchange.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;

/**
 * 付印数据报文体。字段与印单建档一致；金额两位、费率四位，日期 yyyy-MM-dd。
 * 往返转换不新增/不丢弃字段，故用纸估算结果不入报文（由接收方按规范重算）。
 */
@XmlRootElement(name = "FuYinOrder")
@XmlAccessorType(XmlAccessType.FIELD)
public class FuYinOrderData {

    @XmlElement(name = "orderNo")
    private String orderNo;

    @XmlElement(name = "customerName")
    private String customerName;

    @XmlElement(name = "productName")
    private String productName;

    /** 排产机台号（对应机台台账 machine_code） */
    @XmlElement(name = "machineCode")
    private String machineCode;

    @XmlElement(name = "printDate")
    private java.time.LocalDate printDate;

    @XmlElement(name = "printQty")
    private Long printQty;

    @XmlElement(name = "kai")
    private Integer kai;

    @XmlElement(name = "paperLengthMm")
    private Integer paperLengthMm;

    @XmlElement(name = "paperWidthMm")
    private Integer paperWidthMm;

    @XmlElement(name = "gsm")
    private Integer gsm;

    @XmlElement(name = "wasteRate")
    @javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter(RateAdapter.class)
    private BigDecimal wasteRate;

    @XmlElement(name = "addRate")
    @javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter(RateAdapter.class)
    private BigDecimal addRate;

    /** 付印金额（元，两位小数） */
    @XmlElement(name = "amount")
    @javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter(AmountAdapter.class)
    private BigDecimal amount;

    @XmlElement(name = "remark")
    private String remark;

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getMachineCode() { return machineCode; }
    public void setMachineCode(String machineCode) { this.machineCode = machineCode; }
    public java.time.LocalDate getPrintDate() { return printDate; }
    public void setPrintDate(java.time.LocalDate printDate) { this.printDate = printDate; }
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
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
