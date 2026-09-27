package com.evops.exchange.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 费率格式规范：固定四位小数（如 0.0500 表示 5%），四舍五入。
 */
public class RateAdapter extends XmlAdapter<String, BigDecimal> {

    @Override
    public BigDecimal unmarshal(String v) {
        if (v == null || v.trim().isEmpty()) {
            return null;
        }
        return new BigDecimal(v.trim()).setScale(4, RoundingMode.HALF_UP);
    }

    @Override
    public String marshal(BigDecimal v) {
        if (v == null) {
            return null;
        }
        return v.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }
}
