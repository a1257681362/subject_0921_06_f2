package com.evops.exchange.message.adapter;

/** 重量格式规范：定点 3 位小数（kg） */
public class WeightXmlAdapter extends AbstractScaledDecimalXmlAdapter {

    public WeightXmlAdapter() {
        super(3);
    }
}
