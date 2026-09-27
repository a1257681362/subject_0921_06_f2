package com.evops.printorder.dto;

import java.math.BigDecimal;

/** 用纸量估算结果：理论 / 损耗 / 加放 分列，合计与总重量一并给出 */
public class PaperEstimate {
    private long baseSheets;
    private long wasteSheets;
    private long addSheets;
    private long totalSheets;
    private BigDecimal paperWeightKg;

    public PaperEstimate(long baseSheets, long wasteSheets, long addSheets,
                         long totalSheets, BigDecimal paperWeightKg) {
        this.baseSheets = baseSheets;
        this.wasteSheets = wasteSheets;
        this.addSheets = addSheets;
        this.totalSheets = totalSheets;
        this.paperWeightKg = paperWeightKg;
    }

    public long getBaseSheets() { return baseSheets; }
    public long getWasteSheets() { return wasteSheets; }
    public long getAddSheets() { return addSheets; }
    public long getTotalSheets() { return totalSheets; }
    public BigDecimal getPaperWeightKg() { return paperWeightKg; }
}
