package com.evops.printorder.service;

import com.evops.printorder.dto.PaperEstimate;
import com.evops.printorder.entity.PrintOrder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 用纸量估算：按开数与克重估算，损耗、加放单列。
 * 理论用纸（全张纸）= CEIL(印数 / 开数)
 * 损耗用纸 = CEIL(理论 × 损耗率)
 * 加放用纸 = CEIL(理论 × 加放率)
 * 合计 = 理论 + 损耗 + 加放
 * 总重量(kg) = 合计张数 × 单张面积(m²) × 克重(g/m²) / 1000
 */
@Component
public class PaperEstimator {

    public PaperEstimate estimate(long printQty, int kai, int paperLengthMm, int paperWidthMm,
                                  int gsm, BigDecimal wasteRate, BigDecimal addRate) {
        long baseSheets = ceilDiv(printQty, kai);

        BigDecimal base = BigDecimal.valueOf(baseSheets);
        long wasteSheets = ceilRate(base, wasteRate);
        long addSheets = ceilRate(base, addRate);
        long totalSheets = baseSheets + wasteSheets + addSheets;

        // 单张面积 m² = (长mm/1000) × (宽mm/1000)
        BigDecimal areaM2 = BigDecimal.valueOf(paperLengthMm)
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(paperWidthMm)
                        .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP));
        BigDecimal weightKg = BigDecimal.valueOf(totalSheets)
                .multiply(areaM2)
                .multiply(BigDecimal.valueOf(gsm))
                .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);

        return new PaperEstimate(baseSheets, wasteSheets, addSheets, totalSheets, weightKg);
    }

    /** 把估算结果回填到印单实体 */
    public void apply(PrintOrder order, PaperEstimate est) {
        order.setBaseSheets(est.getBaseSheets());
        order.setWasteSheets(est.getWasteSheets());
        order.setAddSheets(est.getAddSheets());
        order.setTotalSheets(est.getTotalSheets());
        order.setPaperWeightKg(est.getPaperWeightKg());
    }

    private static long ceilDiv(long a, long b) {
        return (a + b - 1) / b;
    }

    private static long ceilRate(BigDecimal base, BigDecimal rate) {
        if (rate == null || rate.signum() == 0) {
            return 0L;
        }
        return base.multiply(rate).setScale(0, RoundingMode.CEILING).longValueExact();
    }
}
