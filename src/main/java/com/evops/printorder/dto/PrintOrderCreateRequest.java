package com.evops.printorder.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 印单建档请求。
 * 用纸量按开数与克重估算：理论用纸=CEIL(印数/开数)；损耗、加放按各自比率单列。
 */
public class PrintOrderCreateRequest {
    @NotBlank(message = "印单号不能为空")
    private String orderNo;

    private String customerName;
    private String productName;
    private Long machineId;
    private LocalDate printDate;

    @Positive(message = "印数必须大于0")
    private Long printQty;

    @Positive(message = "开数必须大于0")
    private Integer kai;

    /** 全张纸尺寸 mm */
    @Positive(message = "纸张长度必须大于0")
    private Integer paperLengthMm;

    @Positive(message = "纸张宽度必须大于0")
    private Integer paperWidthMm;

    @Positive(message = "克重必须大于0")
    private Integer gsm;

    /** 损耗率（如 0.03 表示 3%） */
    @PositiveOrZero(message = "损耗率不能为负")
    private BigDecimal wasteRate;

    /** 加放率（如 0.02 表示 2%） */
    @PositiveOrZero(message = "加放率不能为负")
    private BigDecimal addRate;

    private String remark;

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Long getMachineId() { return machineId; }
    public void setMachineId(Long machineId) { this.machineId = machineId; }
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
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
