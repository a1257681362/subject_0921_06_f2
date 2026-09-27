package com.evops.exchange.message.adapter;

/** 金额格式规范：定点 2 位小数（如 12345.60） */
public class AmountXmlAdapter extends AbstractScaledDecimalXmlAdapter {

    public AmountXmlAdapter() {
        super(2);
    }
}
