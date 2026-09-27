package com.evops.exchange.message.adapter;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 小数格式规范基类：定点输出（toPlainString，不用科学计数），
 * 按各自小数位 HALF_UP 归一，保证同一数值序列化文本恒定。
 */
public abstract class AbstractScaledDecimalXmlAdapter extends XmlAdapter<String, BigDecimal> {

    private final int scale;

    protected AbstractScaledDecimalXmlAdapter(int scale) {
        this.scale = scale;
    }

    @Override
    public BigDecimal unmarshal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new BigDecimal(value.trim());
    }

    @Override
    public String marshal(BigDecimal value) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }
}
