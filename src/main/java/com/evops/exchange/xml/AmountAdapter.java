package com.evops.exchange.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额格式规范：固定两位小数（元），四舍五入。
 */
public class AmountAdapter extends XmlAdapter<String, BigDecimal> {

    @Override
    public BigDecimal unmarshal(String v) {
        if (v == null || v.trim().isEmpty()) {
            return null;
        }
        // 不丢精度：按原始标度读入（允许对方给 1234.5 / 1234.500），统一规整为两位
        return new BigDecimal(v.trim()).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String marshal(BigDecimal v) {
        if (v == null) {
            return null;
        }
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
