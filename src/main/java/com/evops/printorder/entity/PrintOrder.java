package com.evops.printorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 印单全量记录。
 * 用纸量按开数与克重估算：理论 base_sheets、损耗 waste_sheets、加放 add_sheets 分列。
 */
@TableName("t_print_order")
public class PrintOrder extends BaseEntity {
    private String orderNo;
    private String customerName;
    private String productName;
    private Long machineId;
    private LocalDate printDate;

    /** 估算输入 */
    private Long printQty;
    private Integer kai;
    private Integer paperLengthMm;
    private Integer paperWidthMm;
    private Integer gsm;
    private BigDecimal wasteRate;
    private BigDecimal addRate;

    /** 估算结果（全张纸，单列） */
    private Long baseSheets;
    private Long wasteSheets;
    private Long addSheets;
    private Long totalSheets;
    private BigDecimal paperWeightKg;

    private String status;
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
