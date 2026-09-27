package com.evops.exchange.message.adapter;

/** 比率格式规范：定点 4 位小数（如 0.0500 表示 5%） */
public class RateXmlAdapter extends AbstractScaledDecimalXmlAdapter {

    public RateXmlAdapter() {
        super(4);
    }
}
